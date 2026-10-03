package org.example.motionville.dto.engagement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewResponse {
    private boolean counted;
    private int requiredWatchSeconds;
    private Long viewId;
    private Long videoId;
    private Long viewerId;
    private Instant viewedAt;
}
