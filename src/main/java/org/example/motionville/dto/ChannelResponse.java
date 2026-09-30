package org.example.motionville.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ChannelResponse {

    private Long id;
    private Long ownerId;
    private String handle;
    private String name;
    private String description;
    private String bannerUrl;
    private Instant createdAt;

}