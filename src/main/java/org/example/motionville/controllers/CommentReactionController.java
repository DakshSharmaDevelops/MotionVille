package org.example.motionville.controllers;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.CommentReactionResponse;
import org.example.motionville.dto.CommentReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.services.CommentReactionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
@RequestMapping("/api/comments")
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    @GetMapping("/{commentId}/reaction-summary")
    public CommentReactionSummary commentReactionSummary(
            @PathVariable Long commentId,
            @RequestParam(required = false) Long userId) {
        return commentReactionService.getCommentReactionSummary(commentId,userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PostMapping("/{commentId}/like")
    public CommentReactionResponse commentReactionLike(
            @PathVariable Long commentId,
            @RequestParam Long userId){
        return commentReactionService.setCommentReaction(commentId,userId, ReactionType.LIKE);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PostMapping("/{commentId}/dislike")
    public CommentReactionResponse commentReactionDislike(
            @PathVariable Long commentId,
            @RequestParam Long userId){
        return commentReactionService.setCommentReaction(commentId,userId, ReactionType.DISLIKE);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @DeleteMapping("/{commentId}/reaction")
    public void deleteCommentReaction(
            @PathVariable Long commentId,
            @RequestParam Long userId){
        commentReactionService.deleteCommentReaction(commentId,userId);
    }
}