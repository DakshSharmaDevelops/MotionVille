package org.example.motionville.dto.account;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerificationRequest {

    @Email(message = "Email must be a valid email address")
    private String email;
}
