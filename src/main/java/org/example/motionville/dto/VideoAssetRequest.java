package org.example.motionville.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VideoAssetRequest(
        @NotBlank String assetUrl,
        @NotBlank @Size(max = 20) String quality,
        @NotBlank @Size(max = 100) String mimeType,
        @NotNull @PositiveOrZero Long sizeBytes
) {
}
