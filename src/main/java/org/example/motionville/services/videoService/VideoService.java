package org.example.motionville.services.videoService;

import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final R2StorageService r2StorageService;
    private final TransactionTemplate transactionTemplate;

    public VideoService(
            VideoRepository videoRepository,
            ChannelRepository channelRepository,
            VideoAssetRepository videoAssetRepository,
            @Lazy R2StorageService r2StorageService,
            PlatformTransactionManager transactionManager
    ) {
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.r2StorageService = r2StorageService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public Video getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Video not found"
                ));
    }

    public void postVideo(Video video) {
        if (video == null
                || video.getChannel() == null
                || video.getChannel().getChannelId() == null
                || video.getTitle() == null
                || video.getTitle().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title and channel ID are required"
            );
        }

        var channel = channelRepository
                .findById(video.getChannel().getChannelId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Channel not found"
                ));

        var now = Instant.now();

        video.setVideoId(null);
        video.setChannel(channel);
        video.setVisibility(VideoVisibility.PRIVATE);
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        video.setDurationSeconds(0);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        video.setPublishedAt(null);

        videoRepository.save(video);
    }

    public void deleteVideo(Long id) {
        if (!videoRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Video not found"
            );
        }

        videoRepository.deleteById(id);
    }

    public List<VideoAsset> playVideo(Long videoId) {
        Video video = getVideoById(videoId);

        List<VideoAsset> assets =
                videoAssetRepository.findByVideo(video);

        if (assets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No playback assets found for this video"
            );
        }

        return assets;
    }

    public List<VideoAsset> videoProcessing(Long videoId) {
        Video video = getVideoById(videoId);

        if (video.getProcessingStatus()
                != VideoProcessingStatus.UPLOADING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Video is already processing, completed, or failed"
            );
        }

        String originalKey =
                "videos/" + videoId + "/original.mp4";

        Path directory = null;
        List<String> generatedKeys = new ArrayList<>();

        try {
            video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
            video.setUpdatedAt(Instant.now());
            videoRepository.save(video);

            directory = Files.createTempDirectory(
                    "motionville-processing-"
            );

            Path original = directory.resolve("original.mp4");


            r2StorageService.download(originalKey, original);

            int sourceHeight = Integer.parseInt(runCommand(
                    directory,
                    30,
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    original.toString()
            ));

            double duration = Double.parseDouble(runCommand(
                    directory,
                    30,
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    original.toString()
            ));

            if (sourceHeight <= 0
                    || !Double.isFinite(duration)
                    || duration <= 0
                    || duration > Integer.MAX_VALUE) {

                throw new IOException("Invalid video metadata");
            }

            List<VideoAsset> assets = new ArrayList<>();


            assets.add(buildAsset(
                    video,
                    original,
                    originalKey,
                    "original"
            ));

            for (int height : new int[]{360, 720}) {


                if (sourceHeight < height) {
                    continue;
                }

                String quality = height + "p";
                Path output = directory.resolve(quality + ".mp4");

                runCommand(
                        directory,
                        7200,
                        "ffmpeg",
                        "-nostdin",
                        "-y",
                        "-v", "error",
                        "-i", original.toString(),
                        "-map", "0:v:0",
                        "-map", "0:a:0?",
                        "-vf", "scale=-2:" + height,
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-crf", "23",
                        "-pix_fmt", "yuv420p",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-movflags", "+faststart",
                        output.toString()
                );

                String outputKey =
                        "videos/" + videoId + "/" + quality + ".mp4";

                // Track the key before uploading for failure cleanup.
                generatedKeys.add(outputKey);

                r2StorageService.upload(
                        output,
                        outputKey,
                        "video/mp4"
                );

                assets.add(buildAsset(
                        video,
                        output,
                        outputKey,
                        quality
                ));
            }

            int durationSeconds = (int) Math.ceil(duration);


            return transactionTemplate.execute(status -> {
                Video currentVideo = getVideoById(videoId);

                for (VideoAsset asset : assets) {
                    asset.setVideo(currentVideo);
                }

                List<VideoAsset> savedAssets =
                        videoAssetRepository.saveAll(assets);

                currentVideo.setDurationSeconds(durationSeconds);
                currentVideo.setProcessingStatus(
                        VideoProcessingStatus.UPLOADED
                );
                currentVideo.setUpdatedAt(Instant.now());

                videoRepository.save(currentVideo);

                return savedAssets;
            });

        } catch (Exception exception) {


            for (String key : generatedKeys) {
                try {
                    r2StorageService.delete(key);
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Video failedVideo = getVideoById(videoId);

                    failedVideo.setProcessingStatus(
                            VideoProcessingStatus.FAILED
                    );
                    failedVideo.setUpdatedAt(Instant.now());

                    videoRepository.save(failedVideo);
                });
            } catch (RuntimeException saveException) {
                exception.addSuppressed(saveException);
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Video processing failed",
                    exception
            );

        } finally {
            deleteTemporaryFiles(directory);
        }
    }

    private VideoAsset buildAsset(
            Video video,
            Path file,
            String key,
            String quality
    ) throws IOException {

        return VideoAsset.builder()
                .video(video)
                .assetUrl(r2StorageService.objectLocator(key))
                .quality(quality)
                .mimeType("video/mp4")
                .sizeBytes(Files.size(file))
                .createdAt(Instant.now())
                .build();
    }

    private String runCommand(
            Path directory,
            long timeoutSeconds,
            String... command
    ) throws IOException, InterruptedException {

        Path logFile = Files.createTempFile(
                directory,
                "video-process-",
                ".log"
        );

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();

        try {
            boolean completed = process.waitFor(
                    timeoutSeconds,
                    TimeUnit.SECONDS
            );

            if (!completed) {
                throw new IOException(
                        command[0] + " timed out"
                );
            }

            String output = Files.readString(logFile).trim();

            if (process.exitValue() != 0) {
                throw new IOException(
                        command[0] + " failed: " + output
                );
            }

            return output;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();

                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void deleteTemporaryFiles(Path directory) {
        if (directory == null) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            List<Path> files = paths
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path file : files) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException exception) {

                }
            }

        } catch (IOException exception) {

        }
    }
}