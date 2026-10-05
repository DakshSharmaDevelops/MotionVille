package org.example.motionville.services.account;

import org.example.motionville.entity.account.RegistrationOtp;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.RegistrationOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class RegistrationOtpService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationOtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationOtpRepository otpRepository;
    private final AppUserRepository userRepository;
    private final EmailValidatorService emailValidatorService;
    private final EmailService emailService;

    private final long otpValidityMinutes;
    private final long resendCooldownSeconds;
    private final int maxOtpsPerHour;

    public RegistrationOtpService(
            RegistrationOtpRepository otpRepository,
            AppUserRepository userRepository,
            EmailValidatorService emailValidatorService,
            EmailService emailService,
            @Value("${motionville.mail.otp-validity-minutes:10}") long otpValidityMinutes,
            @Value("${motionville.mail.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${motionville.mail.max-resends-per-hour:5}") int maxOtpsPerHour) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailValidatorService = emailValidatorService;
        this.emailService = emailService;
        this.otpValidityMinutes = otpValidityMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxOtpsPerHour = maxOtpsPerHour;
    }

    public void sendRegistrationOtp(String email, String username) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Validate real email provider and MX records
        emailValidatorService.validateEmailDomainMx(normalizedEmail);

        // 2. Anti-enumeration / collision check
        if ((username != null && !username.isBlank() && userRepository.existsByUsername(username.trim()))
                || userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this username or email already exists"
            );
        }

        Instant now = Instant.now();

        // 3. Cooldown check against previous OTP
        Optional<RegistrationOtp> lastOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(normalizedEmail);
        if (lastOtp.isPresent()) {
            long elapsed = Duration.between(lastOtp.get().getCreatedAt(), now).toSeconds();
            if (elapsed < resendCooldownSeconds) {
                long wait = resendCooldownSeconds - elapsed;
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + wait + " seconds before requesting another verification code."
                );
            }
        }

        // 4. Hourly request throttle
        Instant oneHourAgo = now.minus(Duration.ofHours(1));
        long count = otpRepository.countByEmailAndCreatedAtAfter(normalizedEmail, oneHourAgo);
        if (count >= maxOtpsPerHour) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification attempts. Please try again later."
            );
        }

        // 5. Invalidate existing active OTPs for this email
        otpRepository.consumeActiveOtpsForEmail(normalizedEmail);

        // 6. Generate 6-digit OTP
        int code = 100000 + RANDOM.nextInt(900000);
        String otpString = String.valueOf(code);
        Instant expiresAt = now.plus(Duration.ofMinutes(otpValidityMinutes));

        RegistrationOtp otp = new RegistrationOtp(normalizedEmail, otpString, expiresAt);
        otpRepository.save(otp);

        // 7. Dispatch email
        emailService.sendOtpEmail(normalizedEmail, otpString);
    }

    public String generateAndSaveOtp(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        otpRepository.consumeActiveOtpsForEmail(normalizedEmail);

        int code = 100000 + RANDOM.nextInt(900000);
        String otpString = String.valueOf(code);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpValidityMinutes));

        RegistrationOtp otp = new RegistrationOtp(normalizedEmail, otpString, expiresAt);
        otpRepository.save(otp);
        return otpString;
    }

    public void verifyRegistrationOtp(String email, String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String trimmedOtp = rawOtp.trim();

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

        if (!otp.getOtp().equals(trimmedOtp)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid verification code. Please check your email and try again."
            );
        }

        // Mark OTP as consumed so it cannot be replayed
        otp.setConsumed(true);
        otpRepository.save(otp);
        log.info("Email '{}' successfully verified via registration OTP", normalizedEmail);
    }
}
