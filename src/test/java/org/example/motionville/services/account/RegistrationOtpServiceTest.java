package org.example.motionville.services.account;

import org.example.motionville.entity.account.RegistrationOtp;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.RegistrationOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RegistrationOtpServiceTest {

    private RegistrationOtpRepository otpRepository;
    private AppUserRepository userRepository;
    private EmailValidatorService emailValidatorService;
    private EmailService emailService;
    private RegistrationOtpService service;

    @BeforeEach
    void setUp() {
        otpRepository = mock(RegistrationOtpRepository.class);
        userRepository = mock(AppUserRepository.class);
        emailValidatorService = mock(EmailValidatorService.class);
        emailService = mock(EmailService.class);

        service = new RegistrationOtpService(
                otpRepository,
                userRepository,
                emailValidatorService,
                emailService,
                10,
                60,
                5
        );
    }

    @Test
    void sendRegistrationOtpGeneratesCodeAndSendsEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("newuser@gmail.com")).thenReturn(Optional.empty());
        when(otpRepository.countByEmailAndCreatedAtAfter(eq("newuser@gmail.com"), any())).thenReturn(0L);

        service.sendRegistrationOtp("newuser@gmail.com", "newuser");

        verify(emailValidatorService).validateEmailDomainMx("newuser@gmail.com");
        verify(otpRepository).consumeActiveOtpsForEmail("newuser@gmail.com");
        verify(otpRepository).save(any(RegistrationOtp.class));
        verify(emailService).sendOtpEmail(eq("newuser@gmail.com"), anyString());
    }

    @Test
    void sendRegistrationOtpEnforcesCooldown() {
        when(userRepository.existsByEmail("user@gmail.com")).thenReturn(false);
        RegistrationOtp recentOtp = new RegistrationOtp("user@gmail.com", "123456", Instant.now().plus(10, ChronoUnit.MINUTES));
        recentOtp.setCreatedAt(Instant.now().minus(20, ChronoUnit.SECONDS)); // Cooldown is 60s

        when(otpRepository.findTopByEmailOrderByCreatedAtDesc("user@gmail.com")).thenReturn(Optional.of(recentOtp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendRegistrationOtp("user@gmail.com", "user"));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Please wait"));
    }

    @Test
    void sendRegistrationOtpRejectsExistingEmailOrUsername() {
        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendRegistrationOtp("existing@gmail.com", "user"));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void verifyRegistrationOtpSucceedsWithMatchingCode() {
        RegistrationOtp otp = new RegistrationOtp("user@gmail.com", "654321", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("user@gmail.com")).thenReturn(Optional.of(otp));

        assertDoesNotThrow(() -> service.verifyRegistrationOtp("user@gmail.com", "654321"));
        assertTrue(otp.isConsumed());
        verify(otpRepository).save(otp);
    }

    @Test
    void verifyRegistrationOtpRejectsMismatchedCode() {
        RegistrationOtp otp = new RegistrationOtp("user@gmail.com", "654321", Instant.now().plus(10, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("user@gmail.com")).thenReturn(Optional.of(otp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyRegistrationOtp("user@gmail.com", "999999"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid verification code"));
    }

    @Test
    void verifyRegistrationOtpRejectsExpiredCode() {
        RegistrationOtp otp = new RegistrationOtp("user@gmail.com", "654321", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("user@gmail.com")).thenReturn(Optional.of(otp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyRegistrationOtp("user@gmail.com", "654321"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("expired"));
    }
}
