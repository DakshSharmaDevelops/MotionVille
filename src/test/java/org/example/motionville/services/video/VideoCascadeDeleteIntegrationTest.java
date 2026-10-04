package org.example.motionville.services.video;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.engagement.VideoView;
import org.example.motionville.entity.engagement.WatchHistory;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.entity.notification.enums.NotificationType;
import org.example.motionville.entity.report.Report;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class VideoCascadeDeleteIntegrationTest {

    @Autowired
    private VideoManagementService videoManagementService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportRepository reportRepository;

    private Video video;
    private AppUser owner;
    private AppUser viewer;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setUsername("owner_" + System.currentTimeMillis());
        owner.setEmail("owner_" + System.currentTimeMillis() + "@example.com");
        owner.setPassword("password123");
        owner.setDisplayName("Owner");
        owner.setRole("USER");
        owner = userRepository.save(owner);

        viewer = new AppUser();
        viewer.setUsername("viewer_" + System.currentTimeMillis());
        viewer.setEmail("viewer_" + System.currentTimeMillis() + "@example.com");
        viewer.setPassword("password123");
        viewer.setDisplayName("Viewer");
        viewer.setRole("USER");
        viewer = userRepository.save(viewer);

        Channel channel = new Channel();
        channel.setName("Owner Channel");
        channel.setHandle("@channel_" + System.currentTimeMillis());
        channel.setOwner(owner);
        channel = channelRepository.save(channel);

        video = new Video();
        video.setTitle("Cascade Delete Test Video");
        video.setChannel(channel);
        video.setDurationSeconds(120);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setPublishedAt(Instant.now());
        video = videoRepository.save(video);
    }

    @Test
    @DisplayName("Video deletion cleans up foreign key dependencies (watch history, views, notifications, reports)")
    void deleteVideo_withDependencies_deletesSuccessfully() {
        // 1. Add comment
        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setAuthor(viewer);
        comment.setBody("Great video!");
        comment = commentRepository.save(comment);

        // 2. Add watch history
        WatchHistory history = new WatchHistory();
        history.setVideo(video);
        history.setUser(viewer);
        history.setLastPositionSeconds(45);
        history.setLastWatchedAt(Instant.now());
        watchHistoryRepository.save(history);

        // 3. Add video view
        VideoView view = new VideoView();
        view.setVideo(video);
        view.setViewer(viewer);
        view.setViewedAt(Instant.now());
        view.setSessionId("session-123");
        videoViewRepository.save(view);

        // 4. Add video notification & comment notification
        Notification videoNotification = new Notification();
        videoNotification.setRecipient(owner);
        videoNotification.setActor(viewer);
        videoNotification.setVideo(video);
        videoNotification.setMessage("New video!");
        videoNotification.setType(NotificationType.NEW_VIDEO);
        notificationRepository.save(videoNotification);

        Notification commentNotification = new Notification();
        commentNotification.setRecipient(owner);
        commentNotification.setActor(viewer);
        commentNotification.setComment(comment);
        commentNotification.setMessage("New comment!");
        commentNotification.setType(NotificationType.NEW_COMMENT);
        notificationRepository.save(commentNotification);

        // 5. Add video report & comment report
        Report videoReport = new Report();
        videoReport.setReporter(viewer);
        videoReport.setVideo(video);
        videoReport.setReason(ReportReason.OTHER);
        videoReport.setStatus(ReportStatus.OPEN);
        reportRepository.save(videoReport);

        Report commentReport = new Report();
        commentReport.setReporter(viewer);
        commentReport.setComment(comment);
        commentReport.setReason(ReportReason.SPAM);
        commentReport.setStatus(ReportStatus.OPEN);
        reportRepository.save(commentReport);

        Long videoId = video.getVideoId();

        // Perform delete
        videoManagementService.delete(videoId);

        // Verify video is gone
        Optional<Video> deletedVideo = videoRepository.findById(videoId);
        assertFalse(deletedVideo.isPresent());

        // Verify all related foreign key rows are gone
        assertTrue(watchHistoryRepository.findByUser_IdAndVideo_VideoId(viewer.getId(), videoId).isEmpty());
        assertTrue(videoViewRepository.findByVideo_VideoIdAndSessionId(videoId, "session-123").isEmpty());
    }

    @Autowired
    private org.example.motionville.controllers.engagement.WatchHistoryController watchHistoryController;

    @Test
    @DisplayName("Watch progress records for authenticated user even if userId is not supplied")
    void recordProgress_authenticatedUser_success() {
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        viewer.getUsername(),
                        null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            org.example.motionville.dto.engagement.WatchHistoryUpdateRequest req =
                    new org.example.motionville.dto.engagement.WatchHistoryUpdateRequest();
            req.setLastPositionSeconds(50);

            org.example.motionville.dto.engagement.WatchHistoryResponse resp =
                    watchHistoryController.recordProgress(video.getVideoId(), req, auth);

            org.junit.jupiter.api.Assertions.assertNotNull(resp);
            org.junit.jupiter.api.Assertions.assertEquals(viewer.getId(), resp.getUserId());
            org.junit.jupiter.api.Assertions.assertEquals(50, resp.getLastPositionSeconds());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
