package org.example.motionville.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChannelUpdateRequest {

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