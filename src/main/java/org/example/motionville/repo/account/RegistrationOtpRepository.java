package org.example.motionville.repo.account;

import org.example.motionville.entity.account.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, Long> {

    Optional<RegistrationOtp> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);

    Optional<RegistrationOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, Instant after);

    @Modifying
    @Query("UPDATE RegistrationOtp o SET o.consumed = true WHERE o.email = :email AND o.consumed = false")
    void consumeActiveOtpsForEmail(@Param("email") String email);

    void deleteAllByEmail(String email);
}
