package org.example.motionville.services;

import org.example.motionville.entity.comment.CommentReaction;

public interface CommentReactionService {
    CommentReaction findCommentReactionSummary(Long commentId, Long userId);
}
