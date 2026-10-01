package org.example.motionville.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.engagement.enums.ReactionType;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionSummary {

    private long likeCount;
    private long dislikeCount;
    private ReactionType userReaction;
}
