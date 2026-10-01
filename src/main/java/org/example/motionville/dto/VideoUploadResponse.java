package org.example.motionville.dto;

public record VideoUploadResponse(
        Long videoId,
        String objectKey,
        String uploadUrl,
        String mimeType,
        String thumbnailObjectKey,
        String thumbnailUploadUrl
) {
}
