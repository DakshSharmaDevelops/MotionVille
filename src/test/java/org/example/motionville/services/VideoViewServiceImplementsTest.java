package org.example.motionville.services;

import org.example.motionville.dto.VideoViewRequest;
import org.example.motionville.dto.VideoViewResponse;
import org.example.motionville.entity.engagement.VideoView;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class VideoViewServiceImplementsTest {

    private VideoViewRepository videoViewRepository;
    private VideoRepository videoRepository;
    private VideoViewServiceImplements service;

    @BeforeEach
    void setUp() {
        videoViewRepository = mock(VideoViewRepository.class);
        videoRepository = mock(VideoRepository.class);
        service = new VideoViewServiceImplements(
                videoViewRepository,
                videoRepository,
                mock(AppUserRepository.class));
    }

    @Test
    void reusesPreviouslyRecordedViewForSameVideoSession() {
        Video video = readyVideo();
        Instant viewedAt = Instant.parse("2026-01-01T00:00:00Z");
        VideoView existing = VideoView.builder()
                .id(8L)
                .video(video)
                .sessionId("01234567-89ab-cdef-0123-456789abcdef")
                .viewedAt(viewedAt)
                .build();
        when(videoRepository.findById(4L)).thenReturn(Optional.of(video));
        when(videoViewRepository.findByVideo_VideoIdAndSessionId(
                4L, existing.getSessionId())).thenReturn(Optional.of(existing));

        VideoViewRequest request = new VideoViewRequest();
        request.setSessionId(existing.getSessionId());
        request.setWatchedSeconds(30);

        VideoViewResponse response = service.recordView(4L, request);

        assertTrue(response.isCounted());
        assertEquals(8L, response.getViewId());
        assertEquals(viewedAt, response.getViewedAt());
        verify(videoViewRepository, never()).save(any(VideoView.class));
    }

    @Test
    void returnsViewCountForExistingVideo() {
        when(videoRepository.existsById(4L)).thenReturn(true);
        when(videoViewRepository.countByVideo_VideoId(4L)).thenReturn(42L);

        assertEquals(42L, service.getViewCount(4L));
    }

    private Video readyVideo() {
        Video video = new Video();
        video.setVideoId(4L);
        video.setDurationSeconds(120);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        return video;
    }
}
