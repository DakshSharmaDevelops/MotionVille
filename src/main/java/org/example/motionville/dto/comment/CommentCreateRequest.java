package org.example.motionville.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {

    @NotNull
    private Long authorId;

    @NotBlank(message = "Comment body must not be blank")
    @Size(max = 10000, message = "Comment body must be at most 10000 characters")
    private String body;

}
