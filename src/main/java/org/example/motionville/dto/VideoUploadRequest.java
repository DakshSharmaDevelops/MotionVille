package org.example.motionville.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.motionville.entity.video.enums.VideoVisibility;

public record VideoUploadRequest(
        @NotNull Long channelId,
        @Positive Long categoryId,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @Size(max = 255) String thumbnailUrl,
        @NotBlank String mimeType,
        @NotNull @Min(1) @Max(524288000) Long sizeBytes,
        @NotNull VideoVisibility visibility
) {
}
