package org.example.motionville.dto.playlist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayListVideoResponse {
    private Long videoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private VideoVisibility visibility;
    private VideoProcessingStatus processingStatus;
    private Integer position;
    private Instant addedAt;
}
