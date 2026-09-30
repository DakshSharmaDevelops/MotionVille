package org.example.motionville.repo.comment;

import org.example.motionville.entity.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideo_VideoIdAndParentCommentIsNullOrderByCreatedAtAsc(Long videoId);
    List<Comment> findByParentComment_IdOrderByCreatedAtAsc(Long parentCommentId);
}
