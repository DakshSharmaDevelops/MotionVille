package org.example.motionville.controllers;

import lombok.RequiredArgsConstructor;
import org.example.motionville.entity.comment.CommentReaction;
import org.example.motionville.services.CommentReactionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentReactionController {

    private final CommentReactionService commentReactionService;

    @GetMapping("/{commentId}/reaction-summary")
    public CommentReaction commentReactionSummary(
                        @PathVariable Long commentId,
                        @RequestParam Long userId) {
        return commentReactionService.findCommentReactionSummary(commentId,userId);
    }

}
