package org.example.motionville.video.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.motionville.video.VideoVisibility;

public record VideoEditForm(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 5000) String description,
        @Size(max = 80) String category,
        @Size(max = 500) String tags,
        @NotNull VideoVisibility visibility) {
}
