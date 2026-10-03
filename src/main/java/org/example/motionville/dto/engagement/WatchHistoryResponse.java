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
public class WatchHistoryResponse {
    private Long id;
    private Long userId;
    private Long videoId;
    private String videoTitle;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer lastPositionSeconds;
    private Instant lastWatchedAt;
}
