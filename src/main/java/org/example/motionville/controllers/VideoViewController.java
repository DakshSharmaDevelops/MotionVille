package org.example.motionville.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.VideoViewRequest;
import org.example.motionville.dto.VideoViewResponse;
import org.example.motionville.services.VideoViewService;
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
}
