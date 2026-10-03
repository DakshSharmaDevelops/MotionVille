package org.example.motionville.services.comment;

import org.example.motionville.dto.comment.CommentReactionResponse;
import org.example.motionville.dto.comment.CommentReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.springframework.transaction.annotation.Transactional;


public interface CommentReactionService {
    CommentReactionSummary getCommentReactionSummary(Long commentId, Long userId);

    CommentReactionResponse setCommentReaction(Long commentId, Long userId, ReactionType reactionType);

    @Transactional
    void deleteCommentReaction(Long commentId, Long userId);
}
