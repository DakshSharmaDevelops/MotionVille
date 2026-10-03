package org.example.motionville.services.live;

import jakarta.annotation.PreDestroy;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.services.AppUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveBroadcastService {
    // Sessions are local-development state. Restarting the backend ends them.
    // MediaMTX handles video bytes; Spring manages ownership and stream access.
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ChannelRepository channels;
    private final AppUserService users;
    private final LiveMediaServer media;
    private final String publishUrl;
    private final String hlsUrl;
    private final String whipUrl;
    private final String viewerBaseUrl;

    public LiveBroadcastService(ChannelRepository channels, AppUserService users, LiveMediaServer media,
            @Value("${motionville.live.publish-url:rtmp://localhost:1935}") String publishUrl,
            @Value("${motionville.live.hls-url:http://localhost:8888}") String hlsUrl,
            @Value("${motionville.live.whip-url:http://localhost:8889}") String whipUrl,
            @Value("${motionville.frontend-public-url:http://localhost:5173}") String viewerBaseUrl) {
        this.channels = channels; this.users = users; this.media = media;
        this.publishUrl = publishUrl.replaceAll("/+$", "");
        this.hlsUrl = hlsUrl.replaceAll("/+$", "");
        this.whipUrl = whipUrl.replaceAll("/+$", "");
        this.viewerBaseUrl = viewerBaseUrl.replaceAll("/+$", "");
    }

    public record Broadcast(String id, Long channelId, String channelName, String title,
                            String status, String playbackUrl, Instant createdAt) {}
    public record Studio(Broadcast broadcast, String serverUrl, String streamKey, String whipUrl, String viewerBaseUrl,
                         String publishUsername, String publishPassword, String managementToken) {}
    private record Session(String id, Long channelId, String channelName, String title,
                           String publishToken, String managementToken, Instant createdAt) {}

    public synchronized Studio create(Long channelId, String title, String username, String password) {
        // The existing app has no server-issued login session. Recheck credentials
        // here rather than trusting a caller-supplied owner ID to start broadcasts.
        var user = users.login(username, password);
        var channel = channels.findById(channelId).orElseThrow(() -> error(404, "Channel not found"));
        if (!channel.getOwner().getId().equals(user.getId())) throw error(403, "This channel belongs to another account");
        Session existing = sessions.values().stream().filter(s -> s.channelId().equals(channelId)).findFirst().orElse(null);
        if (existing != null) return studio(existing); // Reopen an existing studio safely.
        String id = UUID.randomUUID().toString();
        Session session = new Session(id, channelId, channel.getName(), title.trim(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), Instant.now());
        // Allocate the media path first: an offline server must not create a fake session.
        media.create(path(id));
        sessions.put(id, session);
        return new Studio(view(session, false), publishUrl, streamKey(session),
                whipUrl + "/" + path(id) + "/whip", viewerBaseUrl, "broadcaster",
                session.publishToken(), session.managementToken());
    }

    public List<Broadcast> list() {
        return sessions.values().stream().map(s -> view(s, media.ready(path(s.id()))))
                .filter(s -> s.status().equals("LIVE"))
                .sorted(java.util.Comparator.comparing(Broadcast::createdAt).reversed()).toList();
    }

    public Broadcast get(String id) {
        Session session = require(id);
        return view(session, media.ready(path(id)));
    }

    public synchronized void end(String id, String token) {
        Session session = require(id);
        if (!equal(session.managementToken(), token)) throw error(403, "Invalid broadcast management token");
        media.delete(path(id)); // Removes the path and disconnects the broadcaster/readers.
        sessions.remove(id);
    }

    public boolean authorize(String action, String path, String password) {
        if (path == null || !path.startsWith("live-")) return false;
        Session session = sessions.get(path.substring(5));
        if (session == null) return false;
        // Viewer URLs never contain the publishing or management secret.
        if ("read".equals(action)) return true;
        return "publish".equals(action) && equal(session.publishToken(), password);
    }

    private Studio studio(Session s) {
        return new Studio(view(s, media.ready(path(s.id()))),
                publishUrl, streamKey(s), whipUrl + "/" + path(s.id()) + "/whip", viewerBaseUrl,
                "broadcaster", s.publishToken(), s.managementToken());
    }
    private String streamKey(Session s) { return path(s.id()) + "?user=broadcaster&pass=" + s.publishToken(); }
    private String path(String id) { return "live-" + id; }
    private Broadcast view(Session s, boolean ready) {
        return new Broadcast(s.id(), s.channelId(), s.channelName(), s.title(), ready ? "LIVE" : "WAITING",
                hlsUrl + "/" + path(s.id()) + "/index.m3u8", s.createdAt());
    }
    private Session require(String id) {
        Session s = sessions.get(id);
        if (s == null) throw error(404, "This live broadcast has ended or does not exist");
        return s;
    }
    private boolean equal(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
    private ResponseStatusException error(int status, String message) {
        return new ResponseStatusException(HttpStatus.valueOf(status), message);
    }
    @PreDestroy
    public void shutdown() {
        sessions.values().forEach(s -> { try { media.delete(path(s.id())); } catch (RuntimeException ignored) {} });
        sessions.clear();
    }
}
