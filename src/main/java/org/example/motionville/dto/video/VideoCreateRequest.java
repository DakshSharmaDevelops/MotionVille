package org.example.motionville.dto.video;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.motionville.entity.video.enums.VideoVisibility;

public record VideoCreateRequest(
        @NotNull Long channelId,
        Long categoryId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @Size(max = 255) String thumbnailUrl,
        @NotNull @Min(0) Integer durationSeconds,
        @NotNull VideoVisibility visibility
) {
}
