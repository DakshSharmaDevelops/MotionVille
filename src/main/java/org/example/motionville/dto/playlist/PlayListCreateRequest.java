package org.example.motionville.dto.playlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.playlist.enums.PlayListVisibility;

@Getter
@Setter
@NoArgsConstructor
public class PlayListCreateRequest {
    @NotNull
    private Long ownerId;

    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;
    private PlayListVisibility visibility;
}
