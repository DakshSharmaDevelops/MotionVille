package org.example.motionville.security;

import org.example.motionville.dto.comment.CommentCreateRequest;
import org.example.motionville.dto.playlist.PlayListCreateRequest;

import org.example.motionville.dto.video.VideoCreateRequest;

import org.example.motionville.dto.video.VideoProcessingStatusRequest;
import org.example.motionville.dto.video.VideoVisibilityRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;

import org.example.motionville.entity.playlist.PlayList;
import org.example.motionville.entity.video.Video;

import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;

import org.example.motionville.repo.channel.ChannelRepository;

import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.example.motionville.repo.playlist.PlayListRepository;

import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class Developer3SecurityTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private org.example.motionville.services.video.VideoManagementService videoManagementService;

    @Autowired
    private org.example.motionville.services.playlist.PlayListService playListService;

    @Autowired
    private org.example.motionville.services.comment.CommentService commentService;

    @Autowired
    private org.example.motionville.services.channel.SubscriptionService subscriptionService;

    private AppUser alice;
    private AppUser bob;
    private AppUser admin;

    private Channel aliceChannel;
    private Video aliceVideo;
    private PlayList alicePlaylist;

    @BeforeEach
    void setUp() {

        notificationRepository.deleteAll();
        commentRepository.deleteAll();
        playListRepository.deleteAll();
        videoRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();

        Instant now = Instant.now();

        alice = new AppUser();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPassword(passwordEncoder.encode("alicePassword123"));
        alice.setDisplayName("Alice");
        alice.setRole("USER");
        alice.setCreatedAt(now);
        alice.setUpdatedAt(now);
        alice = userRepository.save(alice);

        bob = new AppUser();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setPassword(passwordEncoder.encode("bobPassword123"));
        bob.setDisplayName("Bob");
        bob.setRole("USER");
        bob.setCreatedAt(now);
        bob.setUpdatedAt(now);
        bob = userRepository.save(bob);

        admin = new AppUser();
        admin.setUsername("adminUser");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("adminPassword123"));
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
        aliceChannel = channelRepository.save(channel);

        Video video = new Video();
        video.setChannel(aliceChannel);
        video.setTitle("Alice Video");
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        video.setPublishedAt(now);
        aliceVideo = videoRepository.save(video);

        PlayList playList = new PlayList();
        playList.setOwner(alice);
        playList.setTitle("Alice Playlist");
        playList.setVisibility(org.example.motionville.entity.playlist.enums.PlayListVisibility.PUBLIC);
        playList.setCreatedAt(now);
        playList.setUpdatedAt(now);
        alicePlaylist = playListRepository.save(playList);

        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(AppUser user) {

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testOwnerCanViewVideo() {
        authenticateAs(alice);

        boolean result = authorizationService.canViewVideo(
                aliceVideo.getVideoId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(true, result);
    }

    @Test
    void testNonOwnerCannotViewPrivateVideo() {
        aliceVideo.setVisibility(VideoVisibility.PRIVATE);
        aliceVideo.setPublishedAt(null);
        videoRepository.save(aliceVideo);

        authenticateAs(bob);

        boolean result = authorizationService.canViewVideo(
                aliceVideo.getVideoId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(false, result);
    }

    @Test
    void testAdminCanViewPrivateVideo() {
        aliceVideo.setVisibility(VideoVisibility.PRIVATE);
        aliceVideo.setPublishedAt(null);
        videoRepository.save(aliceVideo);

        authenticateAs(admin);

        boolean result = authorizationService.canViewVideo(
                aliceVideo.getVideoId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(true, result);
    }

    @Test
    void testNonOwnerCannotChangeVideoVisibility() {
        authenticateAs(bob);

        VideoVisibilityRequest request = new VideoVisibilityRequest(VideoVisibility.PRIVATE);

        boolean isOwner = authorizationService.isVideoOwner(
                aliceVideo.getVideoId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(false, isOwner);
    }

    @Test
    void testOwnerCanChangeVideoVisibility() {
        authenticateAs(alice);

        VideoVisibilityRequest request = new VideoVisibilityRequest(VideoVisibility.PRIVATE);

        assertDoesNotThrow(() ->
                videoManagementService.changeVisibility(aliceVideo.getVideoId(), request)
        );
    }

    @Test
    void testOwnerCannotDirectlyChangeProcessingStatusToReady() {
        aliceVideo.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        videoRepository.save(aliceVideo);

        authenticateAs(alice);

        VideoProcessingStatusRequest request =
                new VideoProcessingStatusRequest(VideoProcessingStatus.READY);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                videoManagementService.changeProcessingStatus(aliceVideo.getVideoId(), request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void testOwnerCannotCreatePlaylistForAnotherUser() {
        authenticateAs(alice);

        PlayListCreateRequest request = new PlayListCreateRequest();
        request.setOwnerId(bob.getId());
        request.setTitle("Illegal Playlist");

        boolean isOwner = authorizationService.isUserOwner(
                request.getOwnerId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(false, isOwner);
    }

    @Test
    void testNonOwnerCannotDeleteComment() {
        authenticateAs(alice);

        CommentCreateRequest createRequest = new CommentCreateRequest();
        createRequest.setAuthorId(alice.getId());
        createRequest.setBody("Alice Comment");

        var comment = commentService.createComment(aliceVideo.getVideoId(), createRequest);

        authenticateAs(bob);

        boolean canDelete = authorizationService.canDeleteComment(
                comment.getId(),
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(false, canDelete);
    }

    @Test
    void testSubscriptionTogglingWorksCorrectly() {
        authenticateAs(bob);

        subscriptionService.subscribe(bob.getId(), aliceChannel.getChannelId());
        assertEquals(true, subscriptionService.isSubscribed(bob.getId(), aliceChannel.getChannelId()));

        subscriptionService.unsubscribe(bob.getId(), aliceChannel.getChannelId());
        assertEquals(false, subscriptionService.isSubscribed(bob.getId(), aliceChannel.getChannelId()));
    }
}
