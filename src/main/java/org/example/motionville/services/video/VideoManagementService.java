package org.example.motionville.services.video;

import org.example.motionville.entity.channel.Channel;

import org.example.motionville.dto.video.VideoCreateRequest;
import org.example.motionville.dto.video.VideoProcessingStatusRequest;
import org.example.motionville.dto.video.VideoResponse;
import org.example.motionville.dto.video.VideoUpdateRequest;
import org.example.motionville.dto.video.VideoVisibilityRequest;
import org.example.motionville.entity.video.Category;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.notification.NotificationCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VideoManagementService {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final R2StorageService r2StorageService;
    private final NotificationCreationService notificationCreationService;
    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoViewRepository videoViewRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;

    @Autowired
    public VideoManagementService(
            VideoRepository videoRepository,
            ChannelRepository channelRepository,
            CategoryRepository categoryRepository,
            VideoAssetRepository videoAssetRepository,
            @Lazy R2StorageService r2StorageService,
            NotificationCreationService notificationCreationService,
            WatchHistoryRepository watchHistoryRepository,
            VideoViewRepository videoViewRepository,
            NotificationRepository notificationRepository,
            ReportRepository reportRepository) {
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.categoryRepository = categoryRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.r2StorageService = r2StorageService;
        this.notificationCreationService = notificationCreationService;
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoViewRepository = videoViewRepository;
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
    }

    public VideoManagementService(
            VideoRepository videoRepository,
            ChannelRepository channelRepository,
            CategoryRepository categoryRepository,
            VideoAssetRepository videoAssetRepository,
            @Lazy R2StorageService r2StorageService,
            NotificationCreationService notificationCreationService) {
        this(
                videoRepository,
                channelRepository,
                categoryRepository,
                videoAssetRepository,
                r2StorageService,
                notificationCreationService,
                null,
                null,
                null,
                null);
    }

    public VideoResponse getVideo(Long id) {
        return toResponse(findVideo(id));
    }

    @Transactional
    public VideoResponse create(VideoCreateRequest request) {
        Video video = new Video();
        video.setChannel(channelRepository.findById(request.channelId())
                .orElseThrow(() -> notFound("Channel not found")));
        video.setCategory(resolveCategory(request.categoryId(), request.categoryName()));
        video.setTitle(request.title().trim());
        video.setDescription(trimToNull(request.description()));
        video.setThumbnailUrl(trimToNull(request.thumbnailUrl()));
        video.setDurationSeconds(request.durationSeconds());
        video.setVisibility(request.visibility());
        video.setProcessingStatus(VideoProcessingStatus.UPLOADING);
        video.setPublishedAt(null);
        return toResponse(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse update(Long id, VideoUpdateRequest request) {
        Video video = findVideo(id);
        boolean wasCdnPublished = isCdnPublished(video);
        video.setChannel(channelRepository.findById(request.channelId())
                .orElseThrow(() -> notFound("Channel not found")));
        video.setCategory(resolveCategory(request.categoryId(), request.categoryName()));
        video.setTitle(request.title().trim());
        video.setDescription(trimToNull(request.description()));
        String previousThumbnailUrl = video.getThumbnailUrl();
        String updatedThumbnailUrl = trimToNull(request.thumbnailUrl());
        video.setThumbnailUrl(updatedThumbnailUrl);
        video.setDurationSeconds(request.durationSeconds());
        setVisibility(video, request.visibility());
        if (previousThumbnailUrl != null && !previousThumbnailUrl.equals(updatedThumbnailUrl)) {
            r2StorageService.deleteAfterCommit(List.of(previousThumbnailUrl));
        }
        Video savedVideo = videoRepository.save(video);
        updateCdnPublication(video, wasCdnPublished);
        return toResponse(savedVideo);
    }

    @Transactional
    public void delete(Long id) {
        Video video = findVideo(id);
        List<String> objectLocators = new ArrayList<>();
        objectLocators.add(video.getThumbnailUrl());
        videoAssetRepository.findByVideo(video).stream()
                .map(asset -> asset.getAssetUrl())
                .forEach(objectLocators::add);
        String hlsPrefix = hlsPrefix(id);
        r2StorageService.deleteCdnPrefixAfterCommit(hlsPrefix);
        r2StorageService.deleteOriginPrefixAfterCommit(hlsPrefix);
        r2StorageService.deleteAfterCommit(objectLocators);

        if (notificationRepository != null) {
            notificationRepository.deleteAllByVideo_VideoId(id);
            notificationRepository.deleteAllByComment_Video_VideoId(id);
        }
        if (reportRepository != null) {
            reportRepository.deleteAllByVideo_VideoId(id);
            reportRepository.deleteAllByComment_Video_VideoId(id);
        }
        if (watchHistoryRepository != null) {
            watchHistoryRepository.deleteAllByVideo_VideoId(id);
        }
        if (videoViewRepository != null) {
            videoViewRepository.deleteAllByVideo_VideoId(id);
        }

        videoRepository.delete(video);
    }

    @Transactional
    public VideoResponse publish(Long id) {
        Video video = findVideo(id);
        requireReady(video);
        if (video.getVisibility() == VideoVisibility.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Change visibility to PUBLIC or UNLISTED before publishing"
            );
        }
        boolean newlyPublished = video.getPublishedAt() == null;
        if (newlyPublished) {
            video.setPublishedAt(Instant.now());
        }
        Video savedVideo = videoRepository.save(video);
        if (newlyPublished) notificationCreationService.notifyNewVideo(savedVideo);
        if (r2StorageService.cdnConfigured()
                && isCdnPublished(savedVideo)
                && hasHlsAsset(savedVideo)) {
            r2StorageService.publishCdnPrefixAfterCommit(hlsPrefix(id));
        }
        return toResponse(savedVideo);
    }

    @Transactional
    public VideoResponse unpublish(Long id) {
        Video video = findVideo(id);
        boolean wasCdnPublished = isCdnPublished(video);
        video.setPublishedAt(null);
        Video savedVideo = videoRepository.save(video);
        updateCdnPublication(video, wasCdnPublished);
        return toResponse(savedVideo);
    }

    @Transactional
    public VideoResponse changeVisibility(Long id, VideoVisibilityRequest request) {
        Video video = findVideo(id);
        boolean wasCdnPublished = isCdnPublished(video);
        setVisibility(video, request.visibility());
        Video savedVideo = videoRepository.save(video);
        updateCdnPublication(video, wasCdnPublished);
        return toResponse(savedVideo);
    }

    @Transactional
    public VideoResponse changeProcessingStatus(
            Long id,
            VideoProcessingStatusRequest request) {
        Video video = findVideo(id);
        boolean wasCdnPublished = isCdnPublished(video);
        boolean wasPublished = video.getPublishedAt() != null;
        VideoProcessingStatus current = video.getProcessingStatus();
        VideoProcessingStatus target = request.processingStatus();

        boolean allowed = switch (current) {
            case UPLOADING -> target == VideoProcessingStatus.PROCESSING
                    || target == VideoProcessingStatus.FAILED;
            case PROCESSING -> target == VideoProcessingStatus.READY
                    || target == VideoProcessingStatus.FAILED;
            case READY, UPLOADED, FAILED -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid processing status transition from " + current + " to " + target
            );
        }
        if (target == VideoProcessingStatus.READY
                && videoAssetRepository.findByVideo(video).stream()
                .noneMatch(asset -> "playback".equals(asset.getQuality()))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A playable video asset is required before marking the video ready"
            );
        }

        video.setProcessingStatus(target);
        if (target == VideoProcessingStatus.READY) {
            if (video.getVisibility() != VideoVisibility.PRIVATE
                    && video.getPublishedAt() == null) {
                video.setPublishedAt(Instant.now());
            }
        } else if (target == VideoProcessingStatus.FAILED) {
            video.setPublishedAt(null);
        }
        Video savedVideo = videoRepository.save(video);
        if (!wasPublished && savedVideo.getPublishedAt() != null) {
            notificationCreationService.notifyNewVideo(savedVideo);
        }
        updateCdnPublication(savedVideo, wasCdnPublished);
        return toResponse(savedVideo);
    }

    private void updateCdnPublication(Video video, boolean wasCdnPublished) {
        if (!r2StorageService.cdnConfigured()) {
            return;
        }
        boolean shouldBeCdnPublished = isCdnPublished(video);
        if (wasCdnPublished == shouldBeCdnPublished) {
            return;
        }
        String prefix = hlsPrefix(video.getVideoId());
        if (shouldBeCdnPublished && hasHlsAsset(video)) {
            r2StorageService.publishCdnPrefixAfterCommit(prefix);
        } else if (wasCdnPublished) {
            r2StorageService.deleteCdnPrefixAfterCommit(prefix);
        }
    }

    private boolean isCdnPublished(Video video) {
        return video.getVisibility() == VideoVisibility.PUBLIC
                && video.getPublishedAt() != null;
    }

    private boolean hasHlsAsset(Video video) {
        return videoAssetRepository.findByVideo(video).stream()
                .anyMatch(asset -> "hls".equals(asset.getQuality()));
    }

    private String hlsPrefix(Long videoId) {
        return "videos/" + videoId + "/hls/";
    }

    private void setVisibility(Video video, VideoVisibility visibility) {
        video.setVisibility(visibility);
        if (visibility == VideoVisibility.PRIVATE) {
            video.setPublishedAt(null);
        }
    }

    private void requireReady(Video video) {
        if (!isReady(video)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Video must finish processing before it can be published"
            );
        }
    }

    private boolean isReady(Video video) {
        return video.getProcessingStatus() == VideoProcessingStatus.READY
                || video.getProcessingStatus() == VideoProcessingStatus.UPLOADED;
    }

    private Video findVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> notFound("Video not found"));
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> notFound("Category not found"));
    }

    private Category resolveCategory(Long categoryId, String categoryName) {
        if (categoryId != null) {
            return findCategory(categoryId);
        }
        String trimmedName = trimToNull(categoryName);
        if (trimmedName == null) {
            return null;
        }
        return categoryRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(trimmedName);
                    newCategory.setSlug(trimmedName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""));
                    return categoryRepository.save(newCategory);
                });
    }

    private VideoResponse toResponse(Video video) {
        return VideoResponseMapper.toResponse(video, r2StorageService);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
