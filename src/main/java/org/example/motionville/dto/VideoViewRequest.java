package org.example.motionville.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VideoViewRequest {
    private Long viewerId;

    @NotNull
    @Min(0)
    private Integer watchedSeconds;
}
