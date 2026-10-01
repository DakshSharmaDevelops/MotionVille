package org.example.motionville.services;

import org.example.motionville.dto.CommentReactionResponse;
import org.example.motionville.dto.CommentReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.springframework.transaction.annotation.Transactional;


public interface CommentReactionService {
    CommentReactionSummary getCommentReactionSummary(Long commentId, Long userId);

    CommentReactionResponse setCommentReaction(Long commentId, Long userId, ReactionType reactionType);

    @Transactional
    void deleteCommentReaction(Long commentId, Long userId);
}
