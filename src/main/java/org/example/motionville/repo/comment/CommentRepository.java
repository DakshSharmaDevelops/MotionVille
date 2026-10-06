package org.example.motionville.repo.comment;

import org.example.motionville.entity.comment.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "video", "parentComment"})
    List<Comment> findByVideo_VideoIdAndParentCommentIsNullOrderByCreatedAtAsc(Long videoId);

    @EntityGraph(attributePaths = {"author", "video", "parentComment"})
    List<Comment> findByParentComment_IdOrderByCreatedAtAsc(Long parentCommentId);
}
