package org.example.motionville.dto.engagement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.engagement.enums.ReactionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoReactionResponse {
    private Long videoId;
    private Long userId;
    private ReactionType reaction;
}
