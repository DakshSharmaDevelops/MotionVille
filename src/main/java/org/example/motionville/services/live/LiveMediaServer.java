package org.example.motionville.services.live;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class LiveMediaServer {
    private static final Logger logger = LoggerFactory.getLogger(LiveMediaServer.class);
    private final RestClient client;
    private final boolean autoStart;
    private final String executable;
    private final String configFile;
    private Process managedProcess;

    public LiveMediaServer(
            @Value("${motionville.live.api-url:http://127.0.0.1:9997}") String apiUrl,
            @Value("${motionville.live.auto-start:true}") boolean autoStart,
            @Value("${motionville.live.binary:mediamtx}") String executable,
            @Value("${motionville.live.config:./streaming/mediamtx.yml}") String configFile) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
        this.autoStart = autoStart;
        this.executable = executable;
        this.configFile = configFile;
    }

    @PostConstruct
    public void startIfNeeded() {
        if (!autoStart || isAvailable()) return;

        var config = java.nio.file.Path.of(configFile).toAbsolutePath().normalize();
        if (!java.nio.file.Files.isRegularFile(config)) {
            logger.warn("MediaMTX auto-start skipped: config file '{}' does not exist", config);
            return;
        }

        try {
            managedProcess = new ProcessBuilder(executable, config.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (IOException exception) {
            logger.warn("MediaMTX auto-start failed for '{}': {}. Set MEDIAMTX_PATH to its executable.",
                    executable, exception.getMessage());
            return;
        }

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (isAvailable()) {
                logger.info("Started MediaMTX using '{}'", executable);
                return;
            }
            if (!managedProcess.isAlive()) {
                logger.warn("MediaMTX exited during startup with code {}", managedProcess.exitValue());
                managedProcess = null;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                stopManagedProcess();
                logger.warn("Interrupted while waiting for MediaMTX to start");
                return;
            }
        }

        logger.warn("MediaMTX did not become available within 10 seconds");
        stopManagedProcess();
    }

    private boolean isAvailable() {
        try {
            client.get().uri("/v3/paths/list").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public void create(String path) {
        try {
            client.post().uri("/v3/config/paths/add/" + path)
                    .body(Map.of("source", "publisher", "overridePublisher", false))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException exception) { throw unavailable(exception); }
    }

    public boolean ready(String path) {
        try {
            Map<?, ?> result = client.get().uri("/v3/paths/get/" + path).retrieve().body(Map.class);
            return result != null && Boolean.TRUE.equals(result.get("ready"));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) return false;
            throw unavailable(exception);
        } catch (RuntimeException exception) { throw unavailable(exception); }
    }

    public void delete(String path) {
        try {
            client.delete().uri("/v3/config/paths/delete/" + path).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw unavailable(exception);
        } catch (RuntimeException exception) { throw unavailable(exception); }
    }

    private ResponseStatusException unavailable(RuntimeException cause) {
        logger.warn("MediaMTX API request failed: {}", cause.getMessage());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "MediaMTX is not responding. Ensure it is running and not suspended (for example, by Ctrl+Z), then retry.");
    }

    @PreDestroy
    public void stopManagedProcess() {
        if (managedProcess == null) return;
        managedProcess.destroy();
        try {
            if (!managedProcess.waitFor(3, TimeUnit.SECONDS)) {
                managedProcess.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            managedProcess.destroyForcibly();
        } finally {
            managedProcess = null;
        }
    }
}
