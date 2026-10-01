package org.example.motionville.dto;

import jakarta.validation.constraints.NotNull;
import org.example.motionville.entity.video.enums.VideoVisibility;

public record VideoVisibilityRequest(@NotNull VideoVisibility visibility) {
}
