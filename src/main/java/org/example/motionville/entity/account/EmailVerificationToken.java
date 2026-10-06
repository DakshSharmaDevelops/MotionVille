package org.example.motionville.entity.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verification_tokens_token", columnList = "token"),
        @Index(name = "idx_email_verification_tokens_user", columnList = "user_id")
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(nullable = false)
    private boolean revoked = false;

    public EmailVerificationToken(AppUser user, String token, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isConsumed() {
        return this.consumedAt != null;
    }

    public boolean isValid() {
        return !this.revoked && !isConsumed() && !isExpired();
    }
}
