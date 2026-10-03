package org.example.motionville.dto.video;

import java.util.List;

public record VideoPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<VideoResponse> content
) {
}
