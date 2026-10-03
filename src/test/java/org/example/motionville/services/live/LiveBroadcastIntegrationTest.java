package org.example.motionville.services.live;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Opt-in integration test: isolated H2 database and separate media ports.
@EnabledIfSystemProperty(named="mediamtx.binary", matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        // Security is a test-only dependency in this project; match the runtime classpath.
        "spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "spring.datasource.url=jdbc:h2:mem:live-test;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "motionville.live.auto-start=false",
        "motionville.live.api-url=http://127.0.0.1:19997",
        "motionville.live.publish-url=rtmp://127.0.0.1:11935",
        "motionville.live.hls-url=http://127.0.0.1:18888"
})
class LiveBroadcastIntegrationTest {
    @LocalServerPort int port;
    @Autowired AppUserRepository users;
    @Autowired ChannelRepository channels;
    @TempDir Path directory;

    @Test void broadcastsRealVideoAndRevokesAccessWhenEnded() throws Exception {
        String config = """
                logLevel: warn
                api: true
                apiAddress: 127.0.0.1:19997
                authMethod: http
                authHTTPAddress: http://127.0.0.1:%d/api/live/authorize
                authHTTPExclude:
                  - action: api
                rtsp: false
                rtmpAddress: 127.0.0.1:11935
                hlsAddress: 127.0.0.1:18888
                hlsVariant: mpegts
                hlsAlwaysRemux: true
                hlsSegmentDuration: 1s
                webrtc: false
                srt: false
                moq: false
                paths: {}
                """.formatted(port);
        Path configFile=directory.resolve("mediamtx.yml");
        Files.writeString(configFile, config);
        Process media=new ProcessBuilder(System.getProperty("mediamtx.binary"), configFile.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("media.log").toFile()).start();
        Process publisher=null;
        var api=RestClient.builder().baseUrl("http://127.0.0.1:"+port+"/api/live").build();
        try {
            await(() -> {
                try { RestClient.create().get().uri("http://127.0.0.1:19997/v3/paths/list").retrieve().toBodilessEntity(); return true; }
                catch(Exception e) { return false; }
            }, 10);
            AppUser owner=new AppUser(); owner.setUsername("live-test"); owner.setEmail("live-test@example.test");
            owner.setPassword("test-password"); owner.setPasswordHash("test-password"); owner.setDisplayName("Test broadcaster");
            owner=users.save(owner);
            Channel channel=new Channel(); channel.setOwner(owner);channel.setHandle("@live-test");channel.setName("Test broadcaster");
            channel=channels.save(channel);
            var studio=api.post().body(Map.of("channelId",channel.getChannelId(),"title","Synthetic live test",
                    "username","live-test","password","test-password")).retrieve().body(LiveBroadcastService.Studio.class);
            assertNotNull(studio);
            String id=studio.broadcast().id();
            assertEquals("WAITING",studio.broadcast().status());
            var denied=assertThrows(RestClientResponseException.class, () -> api.post().uri("/authorize")
                    .body(Map.of("action","publish","path","live-"+id,"password","wrong")).retrieve().toBodilessEntity());
            assertEquals(403,denied.getStatusCode().value());
            publisher=new ProcessBuilder("ffmpeg","-nostdin","-v","error","-re","-f","lavfi","-i","testsrc2=size=320x240:rate=15",
                    "-f","lavfi","-i","sine=frequency=440","-t","45","-c:v","libx264","-preset","ultrafast",
                    "-tune","zerolatency","-pix_fmt","yuv420p","-g","15","-c:a","aac","-f","flv",
                    studio.serverUrl()+"/"+studio.streamKey()).redirectErrorStream(true)
                    .redirectOutput(directory.resolve("publisher.log").toFile()).start();
            await(() -> "LIVE".equals(api.get().uri("/"+id).retrieve().body(LiveBroadcastService.Broadcast.class).status()), 20);
            Process viewer=new ProcessBuilder("ffmpeg","-nostdin","-v","error","-i",studio.broadcast().playbackUrl(),
                    "-frames:v","1","-f","null","-").redirectErrorStream(true)
                    .redirectOutput(directory.resolve("viewer.log").toFile()).start();
            try {
                assertTrue(viewer.waitFor(25,TimeUnit.SECONDS),"HLS viewer timed out");
                assertEquals(0,viewer.exitValue(),Files.readString(directory.resolve("viewer.log")));
            } finally { viewer.destroyForcibly(); }
            api.delete().uri("/"+id).header("X-Live-Token",studio.managementToken()).retrieve().toBodilessEntity();
            assertThrows(RestClientResponseException.class, () -> api.get().uri("/"+id).retrieve().toBodilessEntity());
            assertThrows(RestClientResponseException.class, () -> api.post().uri("/authorize")
                    .body(Map.of("action","read","path","live-"+id)).retrieve().toBodilessEntity());
        } finally {
            if(publisher!=null) { publisher.destroyForcibly(); publisher.waitFor(5,TimeUnit.SECONDS); }
            media.destroy(); if(!media.waitFor(5,TimeUnit.SECONDS)) media.destroyForcibly();
        }
    }
    private void await(java.util.function.BooleanSupplier check,int seconds) throws Exception {
        long end=System.nanoTime()+Duration.ofSeconds(seconds).toNanos();
        while(System.nanoTime()<end) { if(check.getAsBoolean()) return; Thread.sleep(200); }
        fail("Timed out waiting for live media state");
    }
}
