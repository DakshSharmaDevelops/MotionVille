package org.example.motionville.repo.engagement;

import org.example.motionville.entity.engagement.VideoReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoReactionRepository extends JpaRepository<VideoReaction, Long> {
    Optional<VideoReaction> findByVideo_VideoIdAndUser_Id(Long videoId, Long userId);
    long countByVideo_VideoIdAndReaction(Long videoId, ReactionType reaction);
}
