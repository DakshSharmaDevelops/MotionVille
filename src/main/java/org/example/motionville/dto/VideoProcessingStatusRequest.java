package org.example.motionville.dto;

import jakarta.validation.constraints.NotNull;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;

public record VideoProcessingStatusRequest(
        @NotNull VideoProcessingStatus processingStatus
) {
}
