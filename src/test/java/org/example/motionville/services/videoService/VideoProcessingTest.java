package org.example.motionville.services.videoService;

import org.example.motionville.dto.VideoPlaybackResponse;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.NotificationCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoProcessingTest {
    @TempDir Path directory;
    VideoRepository videos;
    VideoAssetRepository assets;
    R2StorageService storage;
    ThreadPoolTaskExecutor executor;
    VideoService service;
    Video video;

    @BeforeEach
    void setup() {
        videos = mock(VideoRepository.class);
        assets = mock(VideoAssetRepository.class);
        storage = mock(R2StorageService.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        executor = mock(ThreadPoolTaskExecutor.class);
        service = new VideoService(videos, mock(ChannelRepository.class),
                mock(CategoryRepository.class), assets, storage,
                mock(NotificationCreationService.class), manager, executor);
        ReflectionTestUtils.setField(service, "ffmpeg", "ffmpeg");
        ReflectionTestUtils.setField(service, "ffprobe", "ffprobe");
        video = new Video();
        video.setVideoId(1L);
        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        when(videos.findById(1L)).thenReturn(Optional.of(video));
        when(storage.bucketName()).thenReturn("test");
        when(storage.objectLocator(anyString())).thenAnswer(call -> "r2://test/" + call.getArgument(0));
        when(assets.findByVideo(video)).thenReturn(List.of(VideoAsset.builder()
                .video(video).quality("original").assetUrl("r2://test/videos/1/original")
                .mimeType("application/octet-stream").createdAt(Instant.now()).build()));
        when(assets.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void convertsMkvWithAudioAndKeepsSameVideoId() throws Exception {
        Path source = directory.resolve("input.mkv");
        run("ffmpeg", "-v", "error", "-f", "lavfi", "-i", "color=c=blue:s=640x480:r=10",
                "-f", "lavfi", "-i", "sine=frequency=440", "-t", "0.5",
                "-c:v", "mpeg4", "-c:a", "pcm_s16le", source.toString());
        supplyOriginal(source);
        List<VideoAsset> converted = service.videoProcessing(1L);
        assertEquals(List.of("playback", "360p", "480p", "hls"), converted.stream().map(VideoAsset::getQuality).toList());
        assertTrue(converted.stream().allMatch(asset -> asset.getVideo() == video));
        assertEquals("application/vnd.apple.mpegurl", converted.get(3).getMimeType());
        Path masterPlaylist = directory.resolve("videos_1_hls_v1_master.m3u8");
        assertTrue(Files.exists(masterPlaylist));
        assertTrue(Files.readString(masterPlaylist).contains("360p/index.m3u8"));
        String codecs = run("ffprobe", "-v", "error", "-show_entries", "stream=codec_name,pix_fmt",
                "-of", "default=noprint_wrappers=1", directory.resolve("playback.mp4").toString());
        assertTrue(codecs.contains("h264"));
        assertTrue(codecs.contains("aac"));
        assertTrue(codecs.contains("yuv420p"));
        assertEquals(VideoProcessingStatus.READY, video.getProcessingStatus());
        assertTrue(video.getDurationSeconds() > 0);
    }

    @Test
    void silentAviBelow360pStillGetsPlayableAsset() throws Exception {
        Path source = directory.resolve("input.avi");
        run("ffmpeg", "-v", "error", "-f", "lavfi", "-i", "color=s=320x240:r=10",
                "-t", "0.5", "-c:v", "mpeg4", source.toString());
        supplyOriginal(source);
        assertEquals(List.of("playback", "hls"), service.videoProcessing(1L).stream().map(VideoAsset::getQuality).toList());
        assertEquals(VideoProcessingStatus.READY, video.getProcessingStatus());
    }

    @Test
    void invalidFileFailsWithoutPublishingAssets() throws Exception {
        Path source = directory.resolve("fake.mp4");
        Files.writeString(source, "This is not a video");
        supplyOriginal(source);
        assertThrows(ResponseStatusException.class, () -> service.videoProcessing(1L));
        assertEquals(VideoProcessingStatus.FAILED, video.getProcessingStatus());
        verify(assets, never()).saveAll(anyList());
        verify(storage, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void playbackSelectsConvertedAssetInsteadOfOriginal() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        when(assets.findByVideo(video)).thenReturn(List.of(
                VideoAsset.builder().quality("original").mimeType("video/x-matroska").assetUrl("r2://test/videos/1/original").build(),
                VideoAsset.builder().quality("playback").mimeType("video/mp4").assetUrl("r2://test/videos/1/playback.mp4").build()));
        when(storage.createPlaybackUrl("videos/1/playback.mp4")).thenReturn("test-playback-url");
        assertEquals("test-playback-url", service.getPlayback(1L).assetUrl());
    }

    @Test
    void publicPublishedVideoUsesItsHlsMasterFromTheCdn() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setVisibility(org.example.motionville.entity.video.enums.VideoVisibility.PUBLIC);
        video.setPublishedAt(Instant.now());
        when(storage.cdnConfigured()).thenReturn(true);
        when(storage.cdnObjectExists("videos/1/hls/v1/master.m3u8")).thenReturn(true);
        when(storage.cdnObjectUrl("videos/1/hls/v1/master.m3u8"))
                .thenReturn("https://media.example.test/videos/1/hls/v1/master.m3u8");
        VideoAsset hls = VideoAsset.builder()
                .quality("hls")
                .mimeType("application/vnd.apple.mpegurl")
                .assetUrl("r2://test/videos/1/hls/v1/master.m3u8")
                .sizeBytes(100L)
                .build();
        when(assets.findByVideo(video)).thenReturn(List.of(
                hls,
                VideoAsset.builder().quality("playback").mimeType("video/mp4")
                        .assetUrl("r2://test/videos/1/playback.mp4").build()));

        VideoPlaybackResponse playback = service.getPlayback(1L);

        assertEquals("https://media.example.test/videos/1/hls/v1/master.m3u8", playback.assetUrl());
        assertEquals("application/vnd.apple.mpegurl", playback.mimeType());
        assertEquals("hls", playback.quality());
    }

    @Test
    void privateVideoKeepsUsingSignedMp4InsteadOfCdn() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        when(storage.cdnConfigured()).thenReturn(true);
        when(assets.findByVideo(video)).thenReturn(List.of(
                VideoAsset.builder().quality("hls").mimeType("application/vnd.apple.mpegurl")
                        .assetUrl("r2://test/videos/1/hls/v1/master.m3u8").build(),
                VideoAsset.builder().quality("playback").mimeType("video/mp4")
                        .assetUrl("r2://test/videos/1/playback.mp4").build()));
        when(storage.createPlaybackUrl("videos/1/playback.mp4")).thenReturn("signed-private-url");

        VideoPlaybackResponse playback = service.getPlayback(1L);

        assertEquals("signed-private-url", playback.assetUrl());
        assertEquals("video/mp4", playback.mimeType());
        verify(storage, never()).cdnObjectExists(anyString());
    }

    @Test
    void playbackReturnsTheRequestedQualityAsset() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        when(assets.findByVideo(video)).thenReturn(List.of(
                playbackAsset("playback"),
                playbackAsset("360p"),
                playbackAsset("480p"),
                playbackAsset("720p")));
        when(storage.createPlaybackUrl("videos/1/480p.mp4")).thenReturn("480p-playback-url");

        var response = service.getPlayback(1L, "480p");

        assertEquals("480p", response.quality());
        assertEquals("480p-playback-url", response.assetUrl());
        verify(storage).createPlaybackUrl("videos/1/480p.mp4");
    }

    @Test
    void playbackRejectsUnavailableOrUnsupportedQuality() {
        video.setProcessingStatus(VideoProcessingStatus.READY);
        when(assets.findByVideo(video)).thenReturn(List.of(
                playbackAsset("playback"),
                playbackAsset("360p")));

        ResponseStatusException unavailable = assertThrows(
                ResponseStatusException.class,
                () -> service.getPlayback(1L, "720p"));
        assertEquals(404, unavailable.getStatusCode().value());

        ResponseStatusException unsupported = assertThrows(
                ResponseStatusException.class,
                () -> service.getPlayback(1L, "4k"));
        assertEquals(400, unsupported.getStatusCode().value());
    }

    private VideoAsset playbackAsset(String quality) {
        return VideoAsset.builder()
                .video(video)
                .quality(quality)
                .assetUrl("r2://test/videos/1/" + quality + ".mp4")
                .mimeType("video/mp4")
                .build();
    }

    @Test
    void completionQueuesOnlyAfterCommitAndRejectsDuplicates() {
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        when(videos.findByVideoId(1L)).thenReturn(Optional.of(video));
        VideoAsset original = assets.findByVideo(video).get(0);
        original.setSizeBytes(1000L); // Outside the Long cache: compare values, not references.
        when(storage.headObject("videos/1/original")).thenReturn(
                software.amazon.awssdk.services.s3.model.HeadObjectResponse.builder()
                        .contentLength(1000L).contentType("application/octet-stream").build());
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.completeVideoUpload(1L);
            assertEquals(VideoProcessingStatus.PROCESSING, video.getProcessingStatus());
            verifyNoInteractions(executor);
            assertThrows(ResponseStatusException.class, () -> service.completeVideoUpload(1L));
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(executor).execute(any(Runnable.class));
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void failedStorageUploadCleansPartialOutputAndMarksFailed() throws Exception {
        Path source = directory.resolve("input.avi");
        run("ffmpeg", "-v", "error", "-f", "lavfi", "-i", "color=s=320x240:r=10",
                "-t", "0.5", "-c:v", "mpeg4", source.toString());
        supplyOriginal(source);
        doThrow(new IllegalStateException("Storage unavailable"))
                .when(storage).upload(any(Path.class), anyString(), eq("video/mp4"));
        assertThrows(ResponseStatusException.class, () -> service.videoProcessing(1L));
        assertEquals(VideoProcessingStatus.FAILED, video.getProcessingStatus());
        verify(storage).delete("videos/1/playback.mp4");
        verify(assets, never()).saveAll(anyList());
    }

    private void supplyOriginal(Path source) throws Exception {
        doAnswer(call -> { Files.copy(source, (Path) call.getArgument(1)); return null; })
                .when(storage).download(eq("videos/1/original"), any(Path.class));
        doAnswer(call -> {
            Path file = call.getArgument(0);
            Files.copy(file, directory.resolve(file.getFileName()));
            return "r2://test/" + call.getArgument(1);
        }).when(storage).upload(any(Path.class), anyString(), eq("video/mp4"));
        doAnswer(call -> {
            Path file = call.getArgument(0);
            Path destination = directory.resolve(call.getArgument(1, String.class).replace('/', '_'));
            Files.copy(file, destination);
            return "r2://test/" + call.getArgument(1);
        }).when(storage).upload(any(Path.class), anyString(), anyString(), anyString());
    }

    private String run(String... command) throws Exception {
        Path output = Files.createTempFile(directory, "command", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Media command timed out");
        }
        String log = Files.readString(output);
        assertEquals(0, process.exitValue(), log);
        return log;
    }
}
