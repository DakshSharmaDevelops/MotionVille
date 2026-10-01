package org.example.motionville.dto;

import java.util.List;

public record VideoPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<VideoResponse> content
) {
}
