package org.example.motionville.services.video;

import org.example.motionville.dto.video.VideoResponse;
import org.example.motionville.entity.video.Video;

final class VideoResponseMapper {

    private VideoResponseMapper() {
    }

    static VideoResponse toResponse(Video video) {
        String channelName = null;
        String channelHandle = null;
        String channelAvatarUrl = null;

        if (video.getChannel() != null) {
            channelName = video.getChannel().getName();
            channelHandle = video.getChannel().getHandle();
            channelAvatarUrl = video.getChannel().getAvatarUrl();
        }

        return new VideoResponse(
                video.getVideoId(),
                video.getChannel() != null ? video.getChannel().getChannelId() : null,
                video.getCategory() == null ? null : video.getCategory().getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getVisibility(),
                video.getProcessingStatus(),
                video.getCreatedAt(),
                video.getUpdatedAt(),
                video.getPublishedAt(),
                channelName,
                channelHandle,
                channelAvatarUrl
        );
    }
}
