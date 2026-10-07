package org.example.motionville.services.account;

import org.example.motionville.dto.account.PasswordResetResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.PasswordResetToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.PasswordResetTokenRepository;
import org.example.motionville.repo.account.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final long tokenValidityMinutes;
    private final long resendCooldownSeconds;
    private final int maxResendsPerHour;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${motionville.password-reset.token-validity-minutes:30}") long tokenValidityMinutes,
            @Value("${motionville.password-reset.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${motionville.password-reset.max-resends-per-hour:5}") int maxResendsPerHour) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenValidityMinutes = tokenValidityMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxResendsPerHour = maxResendsPerHour;
    }

    public PasswordResetResponse sendResetLink(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or username is required");
        }

        String query = emailOrUsername.trim();
        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(query);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(query);
        }

        // Generic response for account enumeration protection
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent account identifier: '{}'", query);
            return new PasswordResetResponse(true, "If an account matches that email or username, a reset link has been sent.");
        }

        AppUser user = userOpt.get();
        Instant now = Instant.now();

        // 1. Rate limiting: cooldown check
        Optional<PasswordResetToken> lastTokenOpt = tokenRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (lastTokenOpt.isPresent()) {
            PasswordResetToken lastToken = lastTokenOpt.get();
            long elapsedSeconds = Duration.between(lastToken.getCreatedAt(), now).toSeconds();
            if (elapsedSeconds < resendCooldownSeconds) {
                long waitSeconds = resendCooldownSeconds - elapsedSeconds;
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + waitSeconds + " seconds before requesting another password reset link."
                );
            }
        }

        // 2. Rate limiting: hourly check
        Instant oneHourAgo = now.minus(Duration.ofHours(1));
        long requestsInLastHour = tokenRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);
        if (requestsInLastHour >= maxResendsPerHour) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many password reset requests. Please try again later."
            );
        }

        // 3. Invalidate prior unconsumed reset tokens
        tokenRepository.revokeActiveTokensForUser(user);

        // 4. Generate new secure random token
        String tokenString = generateSecureToken();
        Instant expiresAt = now.plus(Duration.ofMinutes(tokenValidityMinutes));

        PasswordResetToken resetToken = new PasswordResetToken(user, tokenString, expiresAt);
        tokenRepository.save(resetToken);

        // 5. Send password reset email
        emailService.sendPasswordResetEmail(user, tokenString);
        log.info("Password reset token generated and sent for user '{}'", user.getUsername());

        return new PasswordResetResponse(true, "If an account matches that email or username, a reset link has been sent.");
    }

    public PasswordResetResponse validateToken(String tokenString) {
        PasswordResetToken token = findAndValidateToken(tokenString);
        return new PasswordResetResponse(true, "Token is valid for user: " + token.getUser().getUsername());
    }

    public PasswordResetResponse resetPassword(String tokenString, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        PasswordResetToken token = findAndValidateToken(tokenString);
        AppUser user = token.getUser();

        // Update password with BCrypt
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Consume reset token
        token.setConsumedAt(Instant.now());
        tokenRepository.save(token);

        // Revoke active refresh sessions so all existing logins require new password
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password successfully reset for user '{}'", user.getUsername());
        return new PasswordResetResponse(true, "Your password has been successfully reset. You can now sign in with your new password.");
    }

    private PasswordResetToken findAndValidateToken(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }

        PasswordResetToken token = tokenRepository.findByToken(tokenString.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (token.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link is no longer valid. Please request a new one.");
        }

        if (token.isConsumed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has already been used.");
        }

        if (token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has expired. Please request a new one.");
        }

        return token;
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
