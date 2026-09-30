package org.example.motionville.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;


}