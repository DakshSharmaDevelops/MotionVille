package org.example.motionville.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class CommentRequest {

    @NotBlank(message = "Comment body must not be blank")
    @Size(max = 10000, message = "Comment body must be at most 10000 characters")
    @Getter
    @Setter
    private String body;

}
