package org.example.motionville.repo.comment;

import org.example.motionville.entity.comment.CommentReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByComment_CommentIdAndUserId(Long commentId, Long userId);

    Long countByComment_CommentIdAndReaction(Long commentId, ReactionType reaction);
}
