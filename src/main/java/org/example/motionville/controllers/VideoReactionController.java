package org.example.motionville.controllers;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.VideoReactionResponse;
import org.example.motionville.dto.VideoReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.services.VideoReactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
@RequestMapping("/api/videos")
public class VideoReactionController {

    private final VideoReactionService videoReactionService;

    @GetMapping("/{videoId}/reaction-summary")
    public VideoReactionSummary getVideoReactionSummary(
            @PathVariable Long videoId,
            @RequestParam(required = false) Long userId) {
        return videoReactionService.getVideoReactionSummary(videoId, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PostMapping("/{videoId}/like")
    public VideoReactionResponse likeVideo(@PathVariable Long videoId,
                                           @RequestParam Long userId) {
        return videoReactionService.setReaction(videoId, userId, ReactionType.LIKE);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PostMapping("/{videoId}/dislike")
    public VideoReactionResponse dislikeVideo(@PathVariable Long videoId,
                                              @RequestParam Long userId) {
        return videoReactionService.setReaction(videoId, userId, ReactionType.DISLIKE);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @DeleteMapping("/{videoId}/reaction")
    public void deleteVideoReaction(@PathVariable Long videoId,
                                    @RequestParam Long userId) {
        videoReactionService.deleteReaction(videoId, userId);
    }
}