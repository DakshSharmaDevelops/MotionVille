package org.example.motionville.services.account;

import org.example.motionville.dto.account.EmailVerificationResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.EmailVerificationToken;
import org.example.motionville.entity.account.RegistrationOtp;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.EmailVerificationTokenRepository;
import org.example.motionville.repo.account.RegistrationOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final RegistrationOtpRepository otpRepository;
    private final RegistrationOtpService otpService;

    private final long tokenValidityMinutes;
    private final long resendCooldownSeconds;
    private final int maxResendsPerHour;

    @Autowired
    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            AppUserRepository userRepository,
            EmailService emailService,
            RegistrationOtpRepository otpRepository,
            RegistrationOtpService otpService,
            @Value("${motionville.mail.token-validity-minutes:1440}") long tokenValidityMinutes,
            @Value("${motionville.mail.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${motionville.mail.max-resends-per-hour:5}") int maxResendsPerHour) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpRepository = otpRepository;
        this.otpService = otpService;
        this.tokenValidityMinutes = tokenValidityMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxResendsPerHour = maxResendsPerHour;
    }

    public EmailVerificationToken createAndSendVerificationToken(AppUser user) {
        Instant now = Instant.now();

        // 1. Check cooldown window against the most recent token
        Optional<EmailVerificationToken> lastTokenOpt = tokenRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (lastTokenOpt.isPresent()) {
            EmailVerificationToken lastToken = lastTokenOpt.get();
            long elapsedSeconds = Duration.between(lastToken.getCreatedAt(), now).toSeconds();
            if (elapsedSeconds < resendCooldownSeconds) {
                long waitSeconds = resendCooldownSeconds - elapsedSeconds;
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + waitSeconds + " seconds before requesting another verification email."
                );
            }
        }

        // 2. Check hourly limit
        Instant oneHourAgo = now.minus(Duration.ofHours(1));
        long requestsInLastHour = tokenRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);
        if (requestsInLastHour >= maxResendsPerHour) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification requests. Please try again later."
            );
        }

        // 3. Revoke all previous active unconsumed tokens for this user
        tokenRepository.revokeActiveTokensForUser(user);

        // 4. Generate new secure random token
        String tokenString = generateSecureToken();
        Instant expiresAt = now.plus(Duration.ofMinutes(tokenValidityMinutes));

        EmailVerificationToken token = new EmailVerificationToken(user, tokenString, expiresAt);
        EmailVerificationToken savedToken = tokenRepository.save(token);

        // 5. Generate OTP if service available
        String otp = null;
        if (otpService != null) {
            otp = otpService.generateAndSaveOtp(user.getEmail());
        }

        // 6. Send email containing both token and OTP
        emailService.sendVerificationEmail(user, tokenString, otp);

        return savedToken;
    }

    public EmailVerificationResponse verifyEmail(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required");
        }

        EmailVerificationToken token = tokenRepository.findByToken(tokenString.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

        if (token.isRevoked()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This verification link is no longer valid. Please request a new one."
            );
        }

        if (token.isConsumed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This verification token has already been used."
            );
        }

        if (token.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This verification token has expired. Please request a new one."
            );
        }

        AppUser user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setConsumedAt(Instant.now());
        tokenRepository.save(token);

        if (otpRepository != null) {
            otpRepository.consumeActiveOtpsForEmail(user.getEmail().trim().toLowerCase());
        }

        log.info("User '{}' ({}) successfully verified their email via link", user.getUsername(), user.getEmail());

        return new EmailVerificationResponse("Email verified successfully.", true);
    }

    public EmailVerificationResponse verifyOtp(String email, String rawOtp) {
        if (email == null || email.isBlank() || rawOtp == null || rawOtp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and verification code are required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (otpRepository != null) {
            RegistrationOtp otp = otpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No active verification code found for this email. Please request a new code."
                    ));

            if (otp.isExpired()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Verification code has expired. Please request a new code."
                );
            }

            if (!otp.getOtp().equals(rawOtp.trim())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid verification code. Please check your email and try again."
                );
            }

            otp.setConsumed(true);
            otpRepository.save(otp);
        }

        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> userRepository.findByEmail(normalizedEmail)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));

        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.revokeActiveTokensForUser(user);

        log.info("User '{}' ({}) successfully verified their email via OTP", user.getUsername(), user.getEmail());
        return new EmailVerificationResponse("Email verified successfully.", true);
    }

    public EmailVerificationResponse resendVerification(String email, Authentication authentication) {
        // Resolve target user: from authenticated principal if logged in, or lookup by email
        AppUser targetUser = null;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            targetUser = userRepository.findByUsername(authentication.getName())
                    .orElseGet(() -> userRepository.findByEmail(authentication.getName()).orElse(null));
        } else if (email != null && !email.isBlank()) {
            targetUser = userRepository.findByEmail(email.trim()).orElse(null);
        }

        // Anti-account-enumeration: If user does not exist or is already verified,
        // return the generic success response without leaking user state.
        if (targetUser == null || targetUser.isEmailVerified()) {
            return new EmailVerificationResponse(
                    "If this email is associated with an unverified account, a verification link has been sent.",
                    targetUser != null && targetUser.isEmailVerified()
            );
        }

        // Trigger token creation and email dispatch (cooldown/rate limits still apply and throw 429 if abused)
        createAndSendVerificationToken(targetUser);

        return new EmailVerificationResponse(
                "If this email is associated with an unverified account, a verification link has been sent.",
                false
        );
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
