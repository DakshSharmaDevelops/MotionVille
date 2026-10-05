package org.example.motionville.services.account;

import org.example.motionville.dto.account.EmailVerificationResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.EmailVerificationToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    private EmailVerificationTokenRepository tokenRepository;
    private AppUserRepository userRepository;
    private EmailService emailService;
    private EmailVerificationService service;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(EmailVerificationTokenRepository.class);
        userRepository = mock(AppUserRepository.class);
        emailService = mock(EmailService.class);
        service = new EmailVerificationService(tokenRepository, userRepository, emailService, 1440, 60, 5);

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setEmailVerified(false);
    }

    @Test
    void createAndSendVerificationTokenSucceeds() {
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.empty());
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(testUser), any())).thenReturn(0L);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationToken token = service.createAndSendVerificationToken(testUser);

        assertNotNull(token);
        assertEquals(testUser, token.getUser());
        assertFalse(token.isExpired());
        verify(tokenRepository).revokeActiveTokensForUser(testUser);
        verify(emailService).sendVerificationEmail(eq(testUser), anyString(), any());
    }

    @Test
    void enforcesResendCooldown() {
        EmailVerificationToken recentToken = new EmailVerificationToken(testUser, "token123", Instant.now().plus(1, ChronoUnit.DAYS));
        recentToken.setCreatedAt(Instant.now().minus(30, ChronoUnit.SECONDS)); // only 30s ago, cooldown is 60s

        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.of(recentToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createAndSendVerificationToken(testUser));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Please wait"));
    }

    @Test
    void enforcesHourlyLimit() {
        EmailVerificationToken oldToken = new EmailVerificationToken(testUser, "token123", Instant.now().plus(1, ChronoUnit.DAYS));
        oldToken.setCreatedAt(Instant.now().minus(70, ChronoUnit.SECONDS)); // Cooldown passed

        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.of(oldToken));
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(testUser), any())).thenReturn(5L); // Reached 5 per hour limit

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createAndSendVerificationToken(testUser));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Too many verification requests"));
    }

    @Test
    void verifyEmailSuccessfullyMarksUserVerified() {
        EmailVerificationToken token = new EmailVerificationToken(testUser, "valid-token", Instant.now().plus(1, ChronoUnit.DAYS));
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        EmailVerificationResponse response = service.verifyEmail("valid-token");

        assertTrue(response.isVerified());
        assertTrue(testUser.isEmailVerified());
        assertTrue(token.isConsumed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        EmailVerificationToken token = new EmailVerificationToken(testUser, "expired-token", Instant.now().minus(1, ChronoUnit.HOURS));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyEmail("expired-token"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("expired"));
    }

    @Test
    void verifyEmailRejectsRevokedToken() {
        EmailVerificationToken token = new EmailVerificationToken(testUser, "revoked-token", Instant.now().plus(1, ChronoUnit.DAYS));
        token.setRevoked(true);
        when(tokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyEmail("revoked-token"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("no longer valid"));
    }

    @Test
    void verifyEmailRejectsAlreadyUsedTest() {
        EmailVerificationToken token = new EmailVerificationToken(testUser, "used-token", Instant.now().plus(1, ChronoUnit.DAYS));
        token.setConsumedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verifyEmail("used-token"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("already been used"));
    }

    @Test
    void resendVerificationReturnsGenericMessageForNonExistentUser() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        EmailVerificationResponse response = service.resendVerification("nonexistent@example.com", null);

        assertNotNull(response);
        assertFalse(response.isVerified());
        assertTrue(response.getMessage().contains("If this email is associated with an unverified account"));
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void resendVerificationReturnsGenericMessageForAlreadyVerifiedUser() {
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(testUser));

        EmailVerificationResponse response = service.resendVerification("verified@example.com", null);

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertTrue(response.getMessage().contains("If this email is associated with an unverified account"));
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }
}
