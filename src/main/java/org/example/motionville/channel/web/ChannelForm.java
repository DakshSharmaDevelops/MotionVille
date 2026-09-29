package org.example.motionville.channel.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelForm(
        @NotBlank @Size(min = 2, max = 120) String name,
        @Size(max = 2000) String description) {
}
