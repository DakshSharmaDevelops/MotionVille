package org.example.motionville.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*",
                message = "slug must contain letters, numbers, and single hyphens")
        String slug
) {
}
