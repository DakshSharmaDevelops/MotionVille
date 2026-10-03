package org.example.motionville.security;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.entity.notification.enums.NotificationType;
import org.example.motionville.entity.playlist.PlayList;
import org.example.motionville.entity.playlist.enums.PlayListVisibility;
import org.example.motionville.entity.report.Report;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.comment.CommentReactionRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.engagement.VideoReactionRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.example.motionville.repo.playlist.PlayListRepository;
import org.example.motionville.repo.playlist.PlayListVideoRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class Developer3SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    @Autowired
    private VideoReactionRepository videoReactionRepository;

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private PlayListVideoRepository playListVideoRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser alice;
    private AppUser bob;
    private AppUser admin;
    private Video video;
    private Comment aliceComment;
    private PlayList alicePlaylist;
    private Report report;

    @BeforeEach
    void setUpTestData() {
        videoViewRepository.deleteAll();
        watchHistoryRepository.deleteAll();
        videoReactionRepository.deleteAll();
        commentReactionRepository.deleteAll();
        notificationRepository.deleteAll();
        reportRepository.deleteAll();
        commentRepository.deleteAll();
        playListVideoRepository.deleteAll();
        playListRepository.deleteAll();
        videoRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();

        Instant now = Instant.now();

        alice = new AppUser();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPassword("alicePassword123");
        alice.setPasswordHash(passwordEncoder.encode("alicePassword123"));
        alice.setDisplayName("Alice");
        alice.setRole("USER");
        alice.setCreatedAt(now);
        alice.setUpdatedAt(now);
        alice = userRepository.save(alice);

        bob = new AppUser();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setPassword("bobPassword123");
        bob.setPasswordHash(passwordEncoder.encode("bobPassword123"));
        bob.setDisplayName("Bob");
        bob.setRole("USER");
        bob.setCreatedAt(now);
        bob.setUpdatedAt(now);
        bob = userRepository.save(bob);

        admin = new AppUser();
        admin.setUsername("adminUser");
        admin.setEmail("admin@example.com");
        admin.setPassword("adminPassword123");
        admin.setPasswordHash(passwordEncoder.encode("adminPassword123"));
        admin.setDisplayName("Admin");
        admin.setRole("ADMIN");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin = userRepository.save(admin);

        Channel channel = new Channel();
        channel.setName("Alice Channel");
        channel.setHandle("@alice");
        channel.setOwner(alice);
        channel.setCreatedAt(now);
        channel = channelRepository.save(channel);

        video = new Video();
        video.setTitle("Sample Video");
        video.setDescription("Sample Description");
        video.setChannel(channel);
        video.setDurationSeconds(120);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        video.setPublishedAt(now);
        video = videoRepository.save(video);

        aliceComment = new Comment();
        aliceComment.setAuthor(alice);
        aliceComment.setVideo(video);
        aliceComment.setBody("Alice's original comment");
        aliceComment.setCreatedAt(now);
        aliceComment.setUpdatedAt(now);
        aliceComment = commentRepository.save(aliceComment);

        alicePlaylist = new PlayList();
        alicePlaylist.setTitle("Alice's Playlist");
        alicePlaylist.setOwner(alice);
        alicePlaylist.setVisibility(PlayListVisibility.PUBLIC);
        alicePlaylist.setCreatedAt(now);
        alicePlaylist.setUpdatedAt(now);
        alicePlaylist = playListRepository.save(alicePlaylist);

        Notification notification = Notification.builder()
                .recipient(alice)
                .actor(bob)
                .video(video)
                .type(NotificationType.NEW_COMMENT)
                .message("Bob commented on your video")
                .createdAt(now)
                .build();
        notificationRepository.save(notification);

        report = Report.builder()
                .reporter(bob)
                .video(video)
                .reason(ReportReason.SPAM)
                .details("Looks like spam")
                .status(ReportStatus.OPEN)
                .createdAt(now)
                .build();
        report = reportRepository.save(report);
    }

    @Nested
    @DisplayName("1. Unauthenticated & Missing Credentials")
    class UnauthenticatedTests {

        @Test
        @DisplayName("Unauthenticated user cannot create comments (401)")
        void unauthenticated_cannotCreateComment() throws Exception {
            mockMvc.perform(post("/api/videos/" + video.getVideoId() + "/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"authorId\":" + alice.getId() + ",\"body\":\"Anonymous comment\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }

        @Test
        @DisplayName("Unauthenticated user cannot create playlist (401)")
        void unauthenticated_cannotCreatePlaylist() throws Exception {
            mockMvc.perform(post("/api/playlists")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ownerId\":" + alice.getId() + ",\"title\":\"New Playlist\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Unauthenticated user cannot fetch notifications (401)")
        void unauthenticated_cannotFetchNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications?userId=" + alice.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Unauthenticated user cannot fetch watch history (401)")
        void unauthenticated_cannotFetchWatchHistory() throws Exception {
            mockMvc.perform(get("/api/users/" + alice.getId() + "/history"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Unauthenticated user cannot submit report (401)")
        void unauthenticated_cannotSubmitReport() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reporterId\":" + alice.getId() + ",\"videoId\":" + video.getVideoId() + ",\"reason\":\"SPAM\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    @DisplayName("2. Authentication Failure & Credentials")
    class CredentialsTests {

        @Test
        @DisplayName("Invalid credentials at login returns 401 with error message")
        void invalidCredentials_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"wrongPassword\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
        }

        @Test
        @DisplayName("Expired or fake session cookie returns 401")
        void expiredSessionCookie_returns401() throws Exception {
            mockMvc.perform(get("/api/notifications?userId=" + alice.getId())
                            .header("Cookie", "JSESSIONID=EXPIRED_OR_INVALID_SESSION_99999"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("3. USER - Authorized Owner Access")
    class AuthorizedOwnerTests {

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can update her own comment (200)")
        void owner_canUpdateComment() throws Exception {
            mockMvc.perform(put("/api/comments/" + aliceComment.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Alice updated comment text\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.body").value("Alice updated comment text"));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can update her own playlist (200)")
        void owner_canUpdatePlaylist() throws Exception {
            mockMvc.perform(put("/api/playlists/" + alicePlaylist.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Updated Playlist Title\",\"description\":\"Updated Description\",\"visibility\":\"PUBLIC\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Playlist Title"));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can fetch her own notifications (200)")
        void owner_canFetchNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications?userId=" + alice.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can record watch history on her own account (200)")
        void owner_canRecordWatchHistory() throws Exception {
            mockMvc.perform(post("/api/videos/" + video.getVideoId() + "/history")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"userId\":" + alice.getId() + ",\"lastPositionSeconds\":45}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastPositionSeconds").value(45));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can submit a report as herself (201)")
        void owner_canSubmitReport() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reporterId\":" + alice.getId() + ",\"videoId\":" + video.getVideoId() + ",\"reason\":\"HARASSMENT\",\"details\":\"Harassing content\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reporterId").value(alice.getId()));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Alice can delete Bob's comment on her video (204 No Content)")
        void channelOwner_canDeleteCommentOnOwnVideo() throws Exception {
            Comment bobComment = new Comment();
            bobComment.setAuthor(bob);
            bobComment.setVideo(video);
            bobComment.setBody("Bob's comment on Alice video");
            bobComment.setCreatedAt(Instant.now());
            bobComment.setUpdatedAt(Instant.now());
            bobComment = commentRepository.save(bobComment);

            mockMvc.perform(delete("/api/comments/" + bobComment.getId())
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("4. Wrong Owner & Forbidden Requests")
    class WrongOwnerTests {

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot edit Alice's comment (403 Forbidden)")
        void wrongOwner_cannotEditComment() throws Exception {
            mockMvc.perform(put("/api/comments/" + aliceComment.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\":\"Bob malicious hijack\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot delete Alice's comment (403 Forbidden)")
        void wrongOwner_cannotDeleteComment() throws Exception {
            mockMvc.perform(delete("/api/comments/" + aliceComment.getId())
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot edit Alice's playlist (403 Forbidden)")
        void wrongOwner_cannotEditPlaylist() throws Exception {
            mockMvc.perform(put("/api/playlists/" + alicePlaylist.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Bob hijacked playlist\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot fetch Alice's notifications (403 Forbidden)")
        void wrongOwner_cannotFetchNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications?userId=" + alice.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot fetch Alice's watch history (403 Forbidden)")
        void wrongOwner_cannotFetchWatchHistory() throws Exception {
            mockMvc.perform(get("/api/users/" + alice.getId() + "/history"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "bob", roles = {"USER"})
        @DisplayName("Bob cannot submit report claiming to be Alice (403 Forbidden)")
        void wrongOwner_cannotSpoofReporter() throws Exception {
            mockMvc.perform(post("/api/reports")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reporterId\":" + alice.getId() + ",\"videoId\":" + video.getVideoId() + ",\"reason\":\"SPAM\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Standard user cannot access Admin report list (403 Forbidden)")
        void standardUser_cannotAccessAdminReports() throws Exception {
            mockMvc.perform(get("/api/reports?userId=" + alice.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Standard user cannot update report status (403 Forbidden)")
        void standardUser_cannotUpdateReportStatus() throws Exception {
            mockMvc.perform(put("/api/reports/" + report.getId() + "/status?userId=" + alice.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RESOLVED\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Nested
    @DisplayName("5. ADMIN - Elevated Permissions")
    class AdminTests {

        @Test
        @WithMockUser(username = "adminUser", roles = {"ADMIN"})
        @DisplayName("Admin can view all reports (200 OK)")
        void admin_canViewAllReports() throws Exception {
            mockMvc.perform(get("/api/reports?userId=" + admin.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "adminUser", roles = {"ADMIN"})
        @DisplayName("Admin can update report status (200 OK)")
        void admin_canUpdateReportStatus() throws Exception {
            mockMvc.perform(put("/api/reports/" + report.getId() + "/status?userId=" + admin.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RESOLVED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        @Test
        @WithMockUser(username = "adminUser", roles = {"ADMIN"})
        @DisplayName("Admin can delete another user's comment (204 No Content)")
        void admin_canDeleteComment() throws Exception {
            mockMvc.perform(delete("/api/comments/" + aliceComment.getId())
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("6. CSRF Strategy Validation")
    class CsrfTests {

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Mutating POST request without CSRF token is rejected (403 Forbidden)")
        void mutatingRequest_withoutCsrf_rejected() throws Exception {
            mockMvc.perform(post("/api/videos/" + video.getVideoId() + "/like?userId=" + alice.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice", roles = {"USER"})
        @DisplayName("Mutating POST request with valid CSRF token succeeds (200 OK)")
        void mutatingRequest_withCsrf_succeeds() throws Exception {
            mockMvc.perform(post("/api/videos/" + video.getVideoId() + "/like?userId=" + alice.getId())
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("7. CORS & Public Access")
    class CorsAndPublicTests {

        @Test
        @DisplayName("CORS preflight OPTIONS request from configured frontend origin is allowed with cache header")
        void cors_preflightAllowedOrigin() throws Exception {
            mockMvc.perform(options("/api/videos/" + video.getVideoId() + "/comments")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                    .andExpect(header().string("Access-Control-Max-Age", "3600"));
        }

        @Test
        @DisplayName("CORS preflight from unauthorized origin is denied")
        void cors_preflightUnauthorizedOrigin() throws Exception {
            mockMvc.perform(options("/api/videos/" + video.getVideoId() + "/comments")
                            .header("Origin", "http://malicious-attacker.com")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Unauthenticated guest can record video playback view (200 OK)")
        void guest_canRecordVideoView() throws Exception {
            mockMvc.perform(post("/api/videos/" + video.getVideoId() + "/view")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"11111111-2222-3333-4444-555555555555\",\"watchedSeconds\":35}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.counted").value(true));
        }
    }
}
