package org.example.motionville.services.live;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class LiveMediaServer {
    private final RestClient client;

    public LiveMediaServer(@Value("${motionville.live.api-url:http://127.0.0.1:9997}") String apiUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(apiUrl).requestFactory(factory).build();
    }

    public void create(String path) {
        try {
            client.post().uri("/v3/config/paths/add/" + path)
                    .body(Map.of("source", "publisher", "overridePublisher", false))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException exception) { throw unavailable(); }
    }

    public boolean ready(String path) {
        try {
            Map<?, ?> result = client.get().uri("/v3/paths/get/" + path).retrieve().body(Map.class);
            return result != null && Boolean.TRUE.equals(result.get("ready"));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) return false;
            throw unavailable();
        } catch (RuntimeException exception) { throw unavailable(); }
    }

    public void delete(String path) {
        try {
            client.delete().uri("/v3/config/paths/delete/" + path).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw unavailable();
        } catch (RuntimeException exception) { throw unavailable(); }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Live streaming server is unavailable. Start MediaMTX and try again.");
    }
}
