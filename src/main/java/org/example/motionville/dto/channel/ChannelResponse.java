package org.example.motionville.dto.channel;

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
    private String profileImageUrl;
    private Instant createdAt;
    private long videoCount;
    private long viewCount;

}