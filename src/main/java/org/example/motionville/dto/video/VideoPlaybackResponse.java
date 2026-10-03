package org.example.motionville.dto.video;

public record VideoPlaybackResponse(
        Long videoId,
        String assetUrl,
        String mimeType,
        String quality,
        Long sizeBytes
) {
}
