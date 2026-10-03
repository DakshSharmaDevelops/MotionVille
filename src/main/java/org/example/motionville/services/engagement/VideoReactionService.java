package org.example.motionville.services.engagement;

import org.example.motionville.dto.engagement.VideoReactionResponse;
import org.example.motionville.dto.engagement.VideoReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;

public interface VideoReactionService {
    VideoReactionSummary getVideoReactionSummary(Long videoId, Long userId);
    VideoReactionResponse setReaction(Long videoId, Long userId, ReactionType reactionType);
    void deleteReaction(Long videoId, Long userId);
}
