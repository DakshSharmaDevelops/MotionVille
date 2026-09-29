package org.example.motionville.engagement;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "author")
    List<Comment> findByVideoIdAndDeletedFalseOrderByCreatedAtDesc(Long videoId);

    @Override
    @EntityGraph(attributePaths = {"author", "video", "video.channel", "video.channel.owner"})
    java.util.Optional<Comment> findById(Long id);
}
