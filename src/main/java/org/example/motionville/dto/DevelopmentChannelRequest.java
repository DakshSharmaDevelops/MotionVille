package org.example.motionville.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevelopmentChannelRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String handle,
        @Size(max = 500) String description,
        @Size(max = 255) String bannerUrl
) {
}
