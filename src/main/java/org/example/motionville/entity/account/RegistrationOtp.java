package org.example.motionville.entity.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registration_otps", indexes = {
        @Index(name = "idx_registration_otps_email", columnList = "email"),
        @Index(name = "idx_registration_otps_created_at", columnList = "created_at")
})
public class RegistrationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 10)
    private String otp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    public RegistrationOtp(String email, String otp, Instant expiresAt) {
        this.email = email;
        this.otp = otp;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.consumed = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !consumed && !isExpired();
    }
}
