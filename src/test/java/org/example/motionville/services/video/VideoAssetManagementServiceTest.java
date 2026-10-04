package org.example.motionville.services.video;

import org.example.motionville.dto.video.VideoAssetRequest;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoAssetManagementServiceTest {

    private VideoRepository videoRepository;
    private VideoAssetRepository assetRepository;
    private R2StorageService r2StorageService;
    private VideoAssetManagementService service;
    private Video video;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        assetRepository = mock(VideoAssetRepository.class);
        r2StorageService = mock(R2StorageService.class);
        when(r2StorageService.bucketName()).thenReturn("bucket");
        service = new VideoAssetManagementService(videoRepository, assetRepository, r2StorageService);
        video = new Video();
        video.setVideoId(8L);
        when(videoRepository.findById(8L)).thenReturn(Optional.of(video));
        when(assetRepository.save(any(VideoAsset.class))).thenAnswer(call -> {
            VideoAsset asset = call.getArgument(0);
            asset.setId(12L);
            return asset;
        });
    }

    @Test
    void addsAssetMetadata() {
        when(assetRepository.existsByVideo_VideoIdAndQualityAndMimeType(8L, "playback", "video/mp4"))
                .thenReturn(false);

        var response = service.add(8L,
                new VideoAssetRequest("r2://bucket/videos/8/playback.mp4", "playback", "video/mp4", 123L));

        assertEquals(12L, response.id());
        assertEquals(8L, response.videoId());
        assertEquals(123L, response.sizeBytes());
    }

    @Test
    void rejectsDuplicateAssetVariant() {
        when(assetRepository.existsByVideo_VideoIdAndQualityAndMimeType(8L, "playback", "video/mp4"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.add(8L,
                        new VideoAssetRequest("r2://bucket/path", "playback", "video/mp4", 123L)));

        assertEquals(409, exception.getStatusCode().value());
        verify(assetRepository, never()).save(any());
    }

    @Test
    void deletesAssetMetadataAndSchedulesItsR2ObjectForCleanup() {
        VideoAsset asset = VideoAsset.builder().id(12L).video(video).assetUrl("r2://bucket/path")
                .quality("playback").mimeType("video/mp4").sizeBytes(123L).build();
        when(assetRepository.findByIdAndVideo_VideoId(12L, 8L)).thenReturn(Optional.of(asset));

        service.delete(8L, 12L);

        verify(assetRepository).delete(asset);
        verify(r2StorageService).deleteAfterCommit(java.util.List.of("r2://bucket/path"));
    }
}
