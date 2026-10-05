package org.example.motionville.services.video;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Category;

import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.dto.video.VideoPlaybackResponse;
import org.example.motionville.dto.video.VideoUploadRequest;
import org.example.motionville.dto.video.VideoUploadResponse;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.notification.NotificationCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoService {

    // Accept standalone media containers, not playlists that can reference other
    // local files or URLs. Unsupported/corrupt media becomes FAILED.
    private static final String INPUT_FORMATS = "mov,matroska,webm,avi,asf,flv,mpeg,mpegts,ogg";
    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private final ThreadPoolTaskExecutor processingExecutor;
    @Value("${motionville.ffmpeg:ffmpeg}")
    private String ffmpeg;
    @Value("${motionville.ffprobe:ffprobe}")
    private String ffprobe;
    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final R2StorageService r2StorageService;
    private final NotificationCreationService notificationCreationService;
    private final TransactionTemplate transactionTemplate;

    public VideoService(
            VideoRepository videoRepository,
            ChannelRepository channelRepository,
            CategoryRepository categoryRepository,
            VideoAssetRepository videoAssetRepository,
            @Lazy R2StorageService r2StorageService,
            NotificationCreationService notificationCreationService,
            PlatformTransactionManager transactionManager,
            @Qualifier("videoProcessingExecutor") ThreadPoolTaskExecutor processingExecutor
    ) {
        this.processingExecutor = processingExecutor;
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.categoryRepository = categoryRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.r2StorageService = r2StorageService;
        this.notificationCreationService = notificationCreationService;
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

    @Transactional
    public VideoUploadResponse createVideoUpload(VideoUploadRequest request) {
        // MIME type is only an upload hint. ffprobe validates the actual file later.
        // Some browsers report MKV/AVI files as application/octet-stream.
        if (request.mimeType() == null || request.mimeType().length() > 100
                || !(request.mimeType().matches("video/[A-Za-z0-9.+-]+")
                || request.mimeType().equals("application/octet-stream")
                || request.mimeType().equals("application/ogg"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a video file (MP4, WebM, MOV, MKV, AVI, or another supported video format)"
            );
        }

        var channel = channelRepository
                .findById(request.channelId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Channel not found. Select a channel saved in the backend first."
                ));

        var now = Instant.now();
        Video video = new Video();
        video.setChannel(channel);
        if (request.categoryId() != null) {
            video.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category not found"
                    )));
        } else if (request.categoryName() != null && !request.categoryName().trim().isBlank()) {
            String catName = request.categoryName().trim();
            Category category = categoryRepository.findByNameIgnoreCase(catName)
                    .orElseGet(() -> {
                        Category newCat = new Category();
                        newCat.setName(catName);
                        newCat.setSlug(catName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
                        return categoryRepository.save(newCat);
                    });
            video.setCategory(category);
        }
        video.setTitle(request.title().trim());
        video.setDescription(request.description());
        video.setThumbnailUrl(request.thumbnailUrl());
        video.setVisibility(request.visibility());
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        video.setDurationSeconds(0);
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        Video savedVideo = videoRepository.save(video);

        String objectKey = "videos/" + savedVideo.getVideoId() + "/original";
        String thumbnailObjectKey = null;
        String thumbnailUploadUrl = null;
        if (request.thumbnailUrl() == null || request.thumbnailUrl().isBlank()) {
            thumbnailObjectKey = "videos/" + savedVideo.getVideoId() + "/thumbnail.jpg";
            savedVideo.setThumbnailUrl(r2StorageService.objectLocator(thumbnailObjectKey));
            videoRepository.save(savedVideo);
            thumbnailUploadUrl = r2StorageService.createUploadUrl(
                    thumbnailObjectKey,
                    "image/jpeg"
            );
        }

        VideoAsset asset = VideoAsset.builder()
                .video(savedVideo)
                .assetUrl(r2StorageService.objectLocator(objectKey))
                .quality("original")
                .mimeType(request.mimeType())
                .sizeBytes(request.sizeBytes())
                .createdAt(now)
                .build();
        videoAssetRepository.save(asset);

        return new VideoUploadResponse(
                savedVideo.getVideoId(),
                objectKey,
                r2StorageService.createUploadUrl(objectKey, request.mimeType()),
                request.mimeType(),
                thumbnailObjectKey,
                thumbnailUploadUrl
        );
    }

    @Transactional
    public void completeVideoUpload(Long videoId) {
        // Lock this row so two completion requests cannot start duplicate conversions.
        Video video = videoRepository.findByVideoId(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
        if (video.getProcessingStatus() != VideoProcessingStatus.UPLOADING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Video upload is not awaiting completion"
            );
        }

        List<VideoAsset> assets = videoAssetRepository.findByVideo(video);
        if (assets.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Upload asset record is missing or invalid"
            );
        }

        VideoAsset asset = assets.get(0);
        String objectKey = objectKey(asset.getAssetUrl());
        var uploadedObject = r2StorageService.headObject(objectKey);

        if (!java.util.Objects.equals(uploadedObject.contentLength(), asset.getSizeBytes())
                || !asset.getMimeType().equalsIgnoreCase(uploadedObject.contentType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uploaded object size or content type does not match the upload request"
            );
        }

        if (video.getThumbnailUrl() != null
                && video.getThumbnailUrl().startsWith("r2://")) {
            String thumbnailKey = objectKey(video.getThumbnailUrl());
            var thumbnailObject = r2StorageService.headObject(thumbnailKey);
            if (thumbnailObject.contentLength() <= 0
                    || thumbnailObject.contentLength() > 2 * 1024 * 1024
                    || !"image/jpeg".equalsIgnoreCase(thumbnailObject.contentType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Generated thumbnail is missing or invalid"
                );
            }
        }

        video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        video.setUpdatedAt(Instant.now());
        videoRepository.save(video);

        // Start only AFTER the database commits PROCESSING. The HTTP request returns
        // immediately; FFmpeg runs on a bounded worker instead of the request thread.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    processingExecutor.execute(() -> {
                        try {
                            videoProcessing(videoId);
                        } catch (RuntimeException exception) {
                            log.error("Video {} processing failed", videoId, exception);
                        }
                    });
                } catch (RuntimeException rejected) {
                    markFailed(videoId);
                    log.error("Video {} could not be queued", videoId, rejected);
                }
            }
        });
    }

    public VideoProcessingStatus getProcessingStatus(Long videoId) {
        return getVideoById(videoId).getProcessingStatus();
    }

    @Transactional(readOnly = true)
    public String getThumbnailUrl(Long videoId) {
        Video video = getVideoById(videoId);
        String thumbnailUrl = video.getThumbnailUrl();
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Video thumbnail not found");
        }
        if (thumbnailUrl.startsWith("r2://")) {
            return r2StorageService.createPlaybackUrl(objectKey(thumbnailUrl));
        }
        return thumbnailUrl;
    }

    private void markFailed(Long videoId) {
        // REQUIRES_NEW also works when invoked from an afterCommit callback.
        TransactionTemplate failureTransaction = new TransactionTemplate(transactionTemplate.getTransactionManager());
        failureTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        failureTransaction.executeWithoutResult(status -> {
            videoRepository.findById(videoId).ifPresent(video -> {
                video.setProcessingStatus(VideoProcessingStatus.FAILED);
                video.setUpdatedAt(Instant.now());
                videoRepository.save(video);
            });
        });
    }

    @Transactional(readOnly = true)
    public VideoPlaybackResponse getPlayback(Long videoId) {
        return getPlayback(videoId, null);
    }

    @Transactional(readOnly = true)
    public VideoPlaybackResponse getPlayback(Long videoId, String requestedQuality) {
        Video video = getVideoById(videoId);
        if (video.getProcessingStatus() != VideoProcessingStatus.READY
                && video.getProcessingStatus() != VideoProcessingStatus.UPLOADED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Video is not ready for playback"
            );
        }

        List<VideoAsset> assets = videoAssetRepository.findByVideo(video);
        if (assets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No playback assets found for this video"
            );
        }

        VideoAsset asset;
        if (requestedQuality != null && !requestedQuality.isBlank()
                && !"auto".equalsIgnoreCase(requestedQuality)) {
            if (!requestedQuality.matches("(360|480|720|1080)p")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported playback quality"
                );
            }
            asset = assets.stream()
                    .filter(candidate -> requestedQuality.equals(candidate.getQuality()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Requested playback quality is not available"
                    ));
        } else {
            if (video.getVisibility() == VideoVisibility.PUBLIC
                    && video.getPublishedAt() != null
                    && r2StorageService.cdnConfigured()) {
                VideoAsset hlsAsset = assets.stream()
                        .filter(candidate -> "hls".equals(candidate.getQuality()))
                        .findFirst()
                        .orElse(null);
                if (hlsAsset != null) {
                    String hlsKey = objectKey(hlsAsset.getAssetUrl());
                    if (r2StorageService.cdnObjectExists(hlsKey)) {
                        return new VideoPlaybackResponse(
                                videoId,
                                r2StorageService.cdnObjectUrl(hlsKey),
                                hlsAsset.getMimeType(),
                                hlsAsset.getQuality(),
                                hlsAsset.getSizeBytes()
                        );
                    }
                }
            }
            // Prefer the normalized playable asset when no quality is requested.
            asset = assets.stream()
                    .filter(candidate -> "playback".equals(candidate.getQuality()))
                    .findFirst().orElse(assets.get(0));
        }
        String objectKey = objectKey(asset.getAssetUrl());
        return new VideoPlaybackResponse(
                videoId,
                r2StorageService.createPlaybackUrl(objectKey),
                asset.getMimeType(),
                asset.getQuality(),
                asset.getSizeBytes()
        );
    }

    private String objectKey(String locator) {
        String prefix = "r2://" + r2StorageService.bucketName() + "/";
        if (locator == null || !locator.startsWith(prefix)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored video asset has an invalid storage locator"
            );
        }
        return locator.substring(prefix.length());
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = getVideoById(id);
        List<String> objectLocators = new ArrayList<>();
        objectLocators.add(video.getThumbnailUrl());
        videoAssetRepository.findByVideo(video).stream()
                .map(VideoAsset::getAssetUrl)
                .forEach(objectLocators::add);
        String hlsPrefix = "videos/" + id + "/hls/";
        r2StorageService.deleteCdnPrefixAfterCommit(hlsPrefix);
        r2StorageService.deleteOriginPrefixAfterCommit(hlsPrefix);
        r2StorageService.deleteAfterCommit(objectLocators);
        videoRepository.delete(video);
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
                != VideoProcessingStatus.PROCESSING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Video is not queued for processing"
            );
        }

        Path directory = null;
        List<String> generatedKeys = new ArrayList<>();

        try {
            // Use the original asset recorded by the presigned upload flow; its key
            // has no extension because FFmpeg detects the container from its contents.
            VideoAsset originalAsset = videoAssetRepository.findByVideo(video).stream()
                    .filter(asset -> "original".equals(asset.getQuality()))
                    .findFirst().orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "Original upload is missing"));
            String originalKey = objectKey(originalAsset.getAssetUrl());

            video.setProcessingStatus(VideoProcessingStatus.PROCESSING);
            video.setUpdatedAt(Instant.now());
            videoRepository.save(video);

            directory = Files.createTempDirectory(
                    "motionville-processing-"
            );

            Path original = directory.resolve("original");


            r2StorageService.download(originalKey, original);

            int sourceHeight = Integer.parseInt(runCommand(
                    directory,
                    30,
                    ffprobe,
                    "-protocol_whitelist", "file", "-format_whitelist", INPUT_FORMATS,
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    original.toString()
            ));

            double duration = Double.parseDouble(runCommand(
                    directory,
                    30,
                    ffprobe,
                    "-protocol_whitelist", "file", "-format_whitelist", INPUT_FORMATS,
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


            // Always produce a playable version, even when the source is below
            // 360p. Merely renaming an AVI/MKV file to .mp4 would not convert it.
            Path playback = directory.resolve("playback.mp4");
            transcode(directory, original, playback, "scale=trunc(iw/2)*2:trunc(ih/2)*2");
            String playbackKey = "videos/" + videoId + "/playback.mp4";
            generatedKeys.add(playbackKey);
            r2StorageService.upload(playback, playbackKey, "video/mp4");
            assets.add(buildAsset(video, playback, playbackKey, "playback"));

            for (int height : new int[]{360, 480, 720, 1080}) {


                if (sourceHeight < height) {
                    continue;
                }

                String quality = height + "p";
                Path output = directory.resolve(quality + ".mp4");

                transcode(directory, original, output, "scale=-2:" + height);

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

            List<VideoAsset> hlsAssets = createHlsAssets(
                    video, directory, original, sourceHeight, generatedKeys);
            assets.addAll(hlsAssets);

            int durationSeconds = (int) Math.ceil(duration);


            List<VideoAsset> savedAssets = transactionTemplate.execute(status -> {
                Video currentVideo = getVideoById(videoId);
                boolean wasPublished = currentVideo.getPublishedAt() != null;

                for (VideoAsset asset : assets) {
                    asset.setVideo(currentVideo);
                }

                List<VideoAsset> persistedAssets =
                        videoAssetRepository.saveAll(assets);

                currentVideo.setDurationSeconds(durationSeconds);
                currentVideo.setProcessingStatus(VideoProcessingStatus.READY);
                currentVideo.setUpdatedAt(Instant.now());
                if (currentVideo.getVisibility() != VideoVisibility.PRIVATE) {
                    currentVideo.setPublishedAt(Instant.now());
                }
                videoRepository.save(currentVideo);
                if (!wasPublished && currentVideo.getPublishedAt() != null) {
                    notificationCreationService.notifyNewVideo(currentVideo);
                }

                return persistedAssets;
            });
            if (savedAssets != null
                    && video.getVisibility() == VideoVisibility.PUBLIC
                    && r2StorageService.cdnConfigured()) {
                try {
                    r2StorageService.publishCdnPrefix("videos/" + videoId + "/hls/");
                } catch (RuntimeException exception) {
                    log.error("Video {} is ready, but its HLS renditions could not be published to the CDN", videoId, exception);
                }
            }
            return savedAssets;

        } catch (Exception exception) {


            for (String key : generatedKeys) {
                try {
                    r2StorageService.delete(key);
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            try {
                markFailed(videoId);
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

    private List<VideoAsset> createHlsAssets(
            Video video,
            Path directory,
            Path source,
            int sourceHeight,
            List<String> generatedKeys
    ) throws IOException, InterruptedException {
        Path hlsRoot = directory.resolve("hls/v1");
        Files.createDirectories(hlsRoot);
        int maxHeight = sourceHeight - sourceHeight % 2;
        List<Integer> heights = new ArrayList<>();
        for (int height : new int[]{360, 480, 720, 1080}) {
            if (maxHeight >= height) {
                heights.add(height);
            }
        }
        if (heights.isEmpty()) {
            heights.add(Math.max(2, maxHeight));
        }

        StringBuilder master = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
        for (int height : heights) {
            String quality = height + "p";
            Path variantDirectory = hlsRoot.resolve(quality);
            Files.createDirectories(variantDirectory);
            Path playlist = variantDirectory.resolve("index.m3u8");
            transcodeHls(directory, source, playlist, variantDirectory, height);
            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(estimatedBandwidth(height))
                    .append("\n")
                    .append(quality)
                    .append("/index.m3u8\n");
        }

        Path masterPlaylist = hlsRoot.resolve("master.m3u8");
        Files.writeString(masterPlaylist, master, StandardCharsets.UTF_8);
        String hlsKeyPrefix = "videos/" + video.getVideoId() + "/hls/v1/";
        List<Path> hlsFiles;
        try (var paths = Files.walk(hlsRoot)) {
            hlsFiles = paths.filter(Files::isRegularFile)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
        for (Path file : hlsFiles) {
            String relativePath = hlsRoot.relativize(file).toString()
                    .replace(file.getFileSystem().getSeparator(), "/");
            String key = hlsKeyPrefix + relativePath;
            generatedKeys.add(key);
            r2StorageService.upload(
                    file,
                    key,
                    relativePath.endsWith(".m3u8")
                            ? "application/vnd.apple.mpegurl"
                            : "video/mp2t",
                    relativePath.endsWith(".m3u8")
                            ? "public, max-age=60"
                            : "public, max-age=31536000, immutable"
            );
        }

        return List.of(VideoAsset.builder()
                .video(video)
                .assetUrl(r2StorageService.objectLocator(hlsKeyPrefix + "master.m3u8"))
                .quality("hls")
                .mimeType("application/vnd.apple.mpegurl")
                .sizeBytes(Files.size(masterPlaylist))
                .createdAt(Instant.now())
                .build());
    }

    private void transcodeHls(
            Path directory,
            Path source,
            Path playlist,
            Path variantDirectory,
            int height
    ) throws IOException, InterruptedException {
        runCommand(directory, 7200, ffmpeg, "-nostdin", "-y", "-v", "error",
                "-protocol_whitelist", "file", "-format_whitelist", INPUT_FORMATS, "-i", source.toString(),
                "-map", "0:v:0", "-map", "0:a:0?", "-vf", "scale=-2:" + height,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "128k",
                "-sc_threshold", "0", "-force_key_frames", "expr:gte(t,n_forced*6)",
                "-f", "hls", "-hls_time", "6", "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_filename", variantDirectory.resolve("segment_%05d.ts").toString(),
                playlist.toString());
    }

    private int estimatedBandwidth(int height) {
        return switch (height) {
            case 360 -> 800_000;
            case 480 -> 1_200_000;
            case 720 -> 2_500_000;
            case 1080 -> 5_000_000;
            default -> Math.max(400_000, height * height * 5);
        };
    }

    private void transcode(Path directory, Path source, Path output, String scale)
            throws IOException, InterruptedException {
        // H.264 video + AAC audio in MP4 is widely playable in browsers.
        // Optional audio mapping accepts silent videos; yuv420p improves device
        // compatibility and faststart allows playback before the entire download.
        runCommand(directory, 7200, ffmpeg, "-nostdin", "-y", "-v", "error",
                "-protocol_whitelist", "file", "-format_whitelist", INPUT_FORMATS, "-i", source.toString(),
                "-map", "0:v:0", "-map", "0:a:0?", "-vf", scale,
                "-c:v", "libx264", "-preset", "fast", "-crf", "23",
                "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "128k",
                "-movflags", "+faststart", output.toString());
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
