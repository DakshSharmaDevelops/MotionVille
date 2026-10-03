package org.example.motionville.controllers.video;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.engagement.VideoViewRequest;
import org.example.motionville.dto.engagement.VideoViewResponse;
import org.example.motionville.services.engagement.VideoViewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoViewController {

    private final VideoViewService videoViewService;

    @PostMapping("/{videoId}/view")
    public VideoViewResponse recordView(@PathVariable Long videoId,
                                        @Valid @RequestBody VideoViewRequest request) {
        return videoViewService.recordView(videoId, request);
    }

    @GetMapping("/{videoId}/views")
    public long getViewCount(@PathVariable Long videoId) {
        return videoViewService.getViewCount(videoId);
    }
}
