package org.example.motionville.services.videoService;

import org.example.motionville.dto.VideoCreateRequest;
import org.example.motionville.dto.VideoProcessingStatusRequest;
import org.example.motionville.dto.VideoResponse;
import org.example.motionville.dto.VideoUpdateRequest;
import org.example.motionville.dto.VideoVisibilityRequest;
import org.example.motionville.entity.video.Category;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class VideoManagementService {

    private final VideoRepository videoRepository;
    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final VideoAssetRepository videoAssetRepository;

    public VideoManagementService(
            VideoRepository videoRepository,
            ChannelRepository channelRepository,
            CategoryRepository categoryRepository,
            VideoAssetRepository videoAssetRepository) {
        this.videoRepository = videoRepository;
        this.channelRepository = channelRepository;
        this.categoryRepository = categoryRepository;
        this.videoAssetRepository = videoAssetRepository;
    }

    public VideoResponse getVideo(Long id) {
        return toResponse(findVideo(id));
    }

    @Transactional
    public VideoResponse create(VideoCreateRequest request) {
        Video video = new Video();
        video.setChannel(channelRepository.findById(request.channelId())
                .orElseThrow(() -> notFound("Channel not found")));
        video.setCategory(findCategory(request.categoryId()));
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
        video.setChannel(channelRepository.findById(request.channelId())
                .orElseThrow(() -> notFound("Channel not found")));
        video.setCategory(findCategory(request.categoryId()));
        video.setTitle(request.title().trim());
        video.setDescription(trimToNull(request.description()));
        video.setThumbnailUrl(trimToNull(request.thumbnailUrl()));
        video.setDurationSeconds(request.durationSeconds());
        setVisibility(video, request.visibility());
        return toResponse(videoRepository.save(video));
    }

    @Transactional
    public void delete(Long id) {
        videoRepository.delete(findVideo(id));
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
        if (video.getPublishedAt() == null) {
            video.setPublishedAt(Instant.now());
        }
        return toResponse(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse unpublish(Long id) {
        Video video = findVideo(id);
        video.setPublishedAt(null);
        return toResponse(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse changeVisibility(Long id, VideoVisibilityRequest request) {
        Video video = findVideo(id);
        setVisibility(video, request.visibility());
        return toResponse(videoRepository.save(video));
    }

    @Transactional
    public VideoResponse changeProcessingStatus(
            Long id,
            VideoProcessingStatusRequest request) {
        Video video = findVideo(id);
        VideoProcessingStatus current = video.getProcessingStatus();
        VideoProcessingStatus target = request.processingStatus();

        boolean allowed = switch (current) {
            case UPLOADING -> target == VideoProcessingStatus.PROCESSING
                    || target == VideoProcessingStatus.FAILED;
            case PROCESSING -> target == VideoProcessingStatus.READY
                    || target == VideoProcessingStatus.UPLOADED
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
        if (target == VideoProcessingStatus.READY
                || target == VideoProcessingStatus.UPLOADED) {
            if (video.getVisibility() != VideoVisibility.PRIVATE
                    && video.getPublishedAt() == null) {
                video.setPublishedAt(Instant.now());
            }
        } else if (target == VideoProcessingStatus.FAILED) {
            video.setPublishedAt(null);
        }
        return toResponse(videoRepository.save(video));
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

    private VideoResponse toResponse(Video video) {
        return VideoResponseMapper.toResponse(video);
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
