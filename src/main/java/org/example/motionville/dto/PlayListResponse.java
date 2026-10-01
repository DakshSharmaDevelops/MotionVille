package org.example.motionville.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.playlist.enums.PlayListVisibility;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PlayListResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private PlayListVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Long> videoIds;
}
