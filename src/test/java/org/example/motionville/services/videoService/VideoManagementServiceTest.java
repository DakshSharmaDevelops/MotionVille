package org.example.motionville.services.videoService;

import org.example.motionville.dto.VideoCreateRequest;
import org.example.motionville.dto.VideoProcessingStatusRequest;
import org.example.motionville.dto.VideoVisibilityRequest;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
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
    private VideoManagementService service;
    private Video video;

    @BeforeEach
    void setUp() {
        videos = mock(VideoRepository.class);
        channels = mock(ChannelRepository.class);
        assets = mock(VideoAssetRepository.class);
        service = new VideoManagementService(
                videos, channels, mock(CategoryRepository.class), assets);

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
    void unpublishMakesVideoPrivate() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());

        var response = service.unpublish(12L);

        assertEquals(VideoVisibility.PRIVATE, response.visibility());
        assertNull(response.publishedAt());
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
    void readyTransitionRequiresPlaybackAsset() {
        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        when(assets.findByVideo(video)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> service.changeProcessingStatus(
                12L, new VideoProcessingStatusRequest(VideoProcessingStatus.READY)));
    }
}
