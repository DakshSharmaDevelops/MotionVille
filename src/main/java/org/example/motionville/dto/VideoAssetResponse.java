package org.example.motionville.dto;

import java.time.Instant;

public record VideoAssetResponse(
        Long id,
        Long videoId,
        String assetUrl,
        String quality,
        String mimeType,
        Long sizeBytes,
        Instant createdAt
) {
}
