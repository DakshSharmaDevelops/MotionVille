package org.example.motionville.services;

import org.example.motionville.dto.VideoReactionResponse;
import org.example.motionville.dto.VideoReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;

public interface VideoReactionService {
    VideoReactionSummary getVideoReactionSummary(Long videoId, Long userId);
    VideoReactionResponse setReaction(Long videoId, Long userId, ReactionType reactionType);
    void deleteReaction(Long videoId, Long userId);
}
