package org.example.motionville.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChannelCreateRequest {

    @NotNull
    private Long ownerId;

    @NotBlank
    @Size(max = 50)
    private String handle;

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @Size(max = 255)
    private String bannerUrl;

}