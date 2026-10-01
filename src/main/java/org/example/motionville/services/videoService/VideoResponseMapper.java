package org.example.motionville.services.videoService;

import org.example.motionville.dto.VideoResponse;
import org.example.motionville.entity.video.Video;

final class VideoResponseMapper {

    private VideoResponseMapper() {
    }

    static VideoResponse toResponse(Video video) {
        return new VideoResponse(
                video.getVideoId(),
                video.getChannel().getChannelId(),
                video.getCategory() == null ? null : video.getCategory().getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getVisibility(),
                video.getProcessingStatus(),
                video.getCreatedAt(),
                video.getUpdatedAt(),
                video.getPublishedAt()
        );
    }
}
