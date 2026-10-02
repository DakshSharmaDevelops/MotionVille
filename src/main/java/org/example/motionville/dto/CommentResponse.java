package org.example.motionville.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CommentResponse {

    private Long id;

    private Long videoId;

    private Long authorId;

    private String authorDisplayName;

    private Long parentCommentId;

    private String body;

    private boolean deleted;

    private Instant createdAt;

    private Instant updatedAt;

}
