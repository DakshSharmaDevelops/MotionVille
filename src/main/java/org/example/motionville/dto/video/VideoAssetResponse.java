package org.example.motionville.dto.video;

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
