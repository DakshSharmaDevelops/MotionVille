package org.example.motionville.repo.comment;

import org.example.motionville.entity.comment.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
}
