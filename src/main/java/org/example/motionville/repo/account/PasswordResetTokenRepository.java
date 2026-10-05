package org.example.motionville.repo.account;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.token = :token")
    Optional<PasswordResetToken> findByToken(@Param("token") String token);

    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(AppUser user);

    long countByUserAndCreatedAtAfter(AppUser user, Instant after);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.revoked = true WHERE t.user = :user AND t.revoked = false AND t.consumedAt IS NULL")
    void revokeActiveTokensForUser(@Param("user") AppUser user);
}
