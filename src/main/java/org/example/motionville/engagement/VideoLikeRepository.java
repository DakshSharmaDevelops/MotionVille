package org.example.motionville.engagement;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
    Optional<VideoLike> findByUserIdAndVideoId(Long userId, Long videoId);

    long countByVideoId(Long videoId);

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);
}
