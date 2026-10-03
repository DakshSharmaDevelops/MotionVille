package org.example.motionville.services.video;

import org.example.motionville.dto.video.VideoCreateRequest;
import org.example.motionville.dto.video.VideoProcessingStatusRequest;
import org.example.motionville.dto.video.VideoVisibilityRequest;
import org.example.motionville.dto.video.VideoUpdateRequest;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.notification.NotificationCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoManagementServiceTest {

    private VideoRepository videos;
    private ChannelRepository channels;
    private VideoAssetRepository assets;
    private R2StorageService r2StorageService;
    private VideoManagementService service;
    private Video video;

    @BeforeEach
    void setUp() {
        videos = mock(VideoRepository.class);
        channels = mock(ChannelRepository.class);
        assets = mock(VideoAssetRepository.class);
        r2StorageService = mock(R2StorageService.class);
        service = new VideoManagementService(
                videos, channels, mock(CategoryRepository.class), assets, r2StorageService,
                mock(NotificationCreationService.class));

        Channel channel = new Channel();
        channel.setChannelId(4L);
        video = new Video();
        video.setVideoId(12L);
        video.setChannel(channel);
        video.setTitle("Demo");
        video.setDurationSeconds(0);
        video.setVisibility(VideoVisibility.PRIVATE);
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        video.setCreatedAt(Instant.now());
        video.setUpdatedAt(Instant.now());
        when(videos.findById(12L)).thenReturn(Optional.of(video));
        when(videos.save(any(Video.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createStartsPrivateAndUploading() {
        Channel channel = video.getChannel();
        when(channels.findById(4L)).thenReturn(Optional.of(channel));
        when(videos.save(any(Video.class))).thenAnswer(call -> {
            Video saved = call.getArgument(0);
            saved.setVideoId(12L);
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        var response = service.create(new VideoCreateRequest(
                4L, null, "  Demo  ", null, null, 0, VideoVisibility.PRIVATE));

        assertEquals(12L, response.id());
        assertEquals("Demo", response.title());
        assertEquals(VideoVisibility.PRIVATE, response.visibility());
        assertEquals(VideoProcessingStatus.UPLOADING, response.processingStatus());
        assertNull(response.publishedAt());
    }

    @Test
    void publishingRequiresReadyAndNonPrivateVisibility() {
        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        assertThrows(ResponseStatusException.class, () -> service.publish(12L));

        video.setProcessingStatus(VideoProcessingStatus.READY);
        assertThrows(ResponseStatusException.class, () -> service.publish(12L));

        video.setVisibility(VideoVisibility.PUBLIC);
        var response = service.publish(12L);
        assertNotNull(response.publishedAt());
    }

    @Test
    void unpublishKeepsVisibilityAndClearsPublicationTimestamp() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());

        var response = service.unpublish(12L);

        assertEquals(VideoVisibility.PUBLIC, response.visibility());
        assertNull(response.publishedAt());
    }

    @Test
    void canRepublishAfterUnpublishing() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());
        service.unpublish(12L);

        var response = service.publish(12L);

        assertEquals(VideoVisibility.PUBLIC, response.visibility());
        assertNotNull(response.publishedAt());
    }

    @Test
    void visibilityChangeClearsPublicationWhenPrivate() {
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());

        var response = service.changeVisibility(
                12L, new VideoVisibilityRequest(VideoVisibility.PRIVATE));

        assertEquals(VideoVisibility.PRIVATE, response.visibility());
        assertNull(response.publishedAt());
    }

    @Test
    void changingVisibilityDoesNotPublishAnUnpublishedVideo() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(VideoVisibility.PRIVATE);
        video.setPublishedAt(null);

        var response = service.changeVisibility(
                12L, new VideoVisibilityRequest(VideoVisibility.PUBLIC));

        assertEquals(VideoVisibility.PUBLIC, response.visibility());
        assertNull(response.publishedAt());
    }

    @Test
    void readyTransitionRequiresPlaybackAsset() {
        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        when(assets.findByVideo(video)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> service.changeProcessingStatus(
                12L, new VideoProcessingStatusRequest(VideoProcessingStatus.READY)));
    }

    @Test
    void deletingVideoSchedulesItsThumbnailAndAssetsForR2Cleanup() {
        video.setThumbnailUrl("r2://bucket/videos/12/thumbnail.jpg");
        VideoAsset asset = VideoAsset.builder()
                .video(video)
                .assetUrl("r2://bucket/videos/12/playback.mp4")
                .quality("playback")
                .mimeType("video/mp4")
                .build();
        when(assets.findByVideo(video)).thenReturn(List.of(asset));

        service.delete(12L);

        verify(r2StorageService).deleteAfterCommit(List.of(
                "r2://bucket/videos/12/thumbnail.jpg",
                "r2://bucket/videos/12/playback.mp4"));
        verify(videos).delete(video);
        verify(r2StorageService).deleteOriginPrefixAfterCommit("videos/12/hls/");
    }

    @Test
    void replacingThumbnailSchedulesOldR2ThumbnailForCleanup() {
        video.setThumbnailUrl("r2://bucket/videos/12/old-thumbnail.jpg");
        when(channels.findById(4L)).thenReturn(Optional.of(video.getChannel()));

        service.update(12L, new VideoUpdateRequest(
                4L, null, "Demo", null, "https://cdn.example/new.jpg",
                0, VideoVisibility.PRIVATE));

        verify(r2StorageService).deleteAfterCommit(
                List.of("r2://bucket/videos/12/old-thumbnail.jpg"));
    }

    @Test
    void publishingPublicVideoWithHlsSchedulesItsRenditionsForCdn() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(VideoVisibility.PUBLIC);
        when(r2StorageService.cdnConfigured()).thenReturn(true);
        when(assets.findByVideo(video)).thenReturn(List.of(VideoAsset.builder()
                .video(video)
                .quality("hls")
                .mimeType("application/vnd.apple.mpegurl")
                .assetUrl("r2://bucket/videos/12/hls/v1/master.m3u8")
                .build()));

        service.publish(12L);

        verify(r2StorageService).publishCdnPrefixAfterCommit("videos/12/hls/");
    }

    @Test
    void unpublishingVideoRemovesItsCdnRenditions() {
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());
        when(r2StorageService.cdnConfigured()).thenReturn(true);

        service.unpublish(12L);

        verify(r2StorageService).deleteCdnPrefixAfterCommit("videos/12/hls/");
    }
}
