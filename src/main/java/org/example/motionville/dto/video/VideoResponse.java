package org.example.motionville.dto.video;

import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;

import java.time.Instant;

public record VideoResponse(
        Long id,
        Long channelId,
        Long categoryId,
        String title,
        String description,
        String thumbnailUrl,
        Integer durationSeconds,
        VideoVisibility visibility,
        VideoProcessingStatus processingStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
