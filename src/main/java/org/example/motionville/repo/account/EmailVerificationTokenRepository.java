package org.example.motionville.repo.account;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findAllByUserAndRevokedFalseAndConsumedAtIsNull(AppUser user);

    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(AppUser user);

    long countByUserAndCreatedAtAfter(AppUser user, Instant after);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false AND t.consumedAt IS NULL")
    void revokeActiveTokensForUser(@Param("user") AppUser user);

    void deleteAllByUser(AppUser user);
}
