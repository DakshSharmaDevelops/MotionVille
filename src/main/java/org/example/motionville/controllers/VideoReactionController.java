package org.example.motionville.controllers;

import lombok.RequiredArgsConstructor;
import org.example.motionville.entity.engagement.VideoReaction;
import org.example.motionville.services.VideoReactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VideoReactionController {

    private final VideoReactionService videoReactionService;

    @GetMapping("/videos/{videoId}/reaction-summary")
    public List<VideoReaction> getVideoReactionSummary(@PathVariable Long videoId) {
        return null;
    }
}
