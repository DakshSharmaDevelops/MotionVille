package org.example.motionville.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PlayListReorderRequest {
    @NotNull
    private List<@NotNull Long> videoIds;
}
