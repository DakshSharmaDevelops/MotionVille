package org.example.motionville.dto.video;

import jakarta.validation.constraints.NotNull;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;

public record VideoProcessingStatusRequest(
        @NotNull VideoProcessingStatus processingStatus
) {
}
