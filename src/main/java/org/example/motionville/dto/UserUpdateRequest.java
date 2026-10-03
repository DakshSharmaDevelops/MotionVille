package org.example.motionville.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserUpdateRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String displayName;

    private String avatarUrl;


}