package org.example.motionville.services.account;

import org.example.motionville.dto.account.PasswordResetResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.PasswordResetToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.PasswordResetTokenRepository;
import org.example.motionville.repo.account.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private PasswordResetTokenRepository tokenRepository;
    private AppUserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private PasswordResetService service;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(PasswordResetTokenRepository.class);
        userRepository = mock(AppUserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);

        service = new PasswordResetService(
                tokenRepository,
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                emailService,
                30,
                60,
                5
        );

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setEmail("alice@example.com");
        testUser.setPassword("$2a$10$oldhashedpassword");
    }

    @Test
    void sendResetLinkSucceedsForValidUser() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.empty());
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(testUser), any())).thenReturn(0L);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResponse response = service.sendResetLink("alice@example.com");

        assertTrue(response.isSuccess());
        verify(tokenRepository).revokeActiveTokensForUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(testUser), anyString());
    }

    @Test
    void sendResetLinkProtectsAgainstAccountEnumeration() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@example.com")).thenReturn(Optional.empty());

        PasswordResetResponse response = service.sendResetLink("unknown@example.com");

        assertTrue(response.isSuccess());
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void sendResetLinkEnforcesCooldown() {
        PasswordResetToken recent = new PasswordResetToken(testUser, "token123", Instant.now().plus(30, ChronoUnit.MINUTES));
        recent.setCreatedAt(Instant.now().minus(20, ChronoUnit.SECONDS)); // Cooldown is 60s

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.of(recent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendResetLink("alice@example.com"));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Please wait"));
    }

    @Test
    void sendResetLinkEnforcesHourlyLimit() {
        PasswordResetToken old = new PasswordResetToken(testUser, "token123", Instant.now().plus(30, ChronoUnit.MINUTES));
        old.setCreatedAt(Instant.now().minus(75, ChronoUnit.SECONDS)); // Cooldown passed

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(testUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(testUser)).thenReturn(Optional.of(old));
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(testUser), any())).thenReturn(5L); // 5/hr max

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendResetLink("alice@example.com"));
        assertEquals(429, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Too many password reset requests"));
    }

    @Test
    void validateTokenSucceedsForValidToken() {
        PasswordResetToken token = new PasswordResetToken(testUser, "valid-token", Instant.now().plus(30, ChronoUnit.MINUTES));
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        PasswordResetResponse response = service.validateToken("valid-token");

        assertTrue(response.isSuccess());
    }

    @Test
    void validateTokenFailsForExpiredToken() {
        PasswordResetToken token = new PasswordResetToken(testUser, "expired-token", Instant.now().minus(5, ChronoUnit.MINUTES));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.validateToken("expired-token"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("expired"));
    }

    @Test
    void validateTokenFailsForConsumedToken() {
        PasswordResetToken token = new PasswordResetToken(testUser, "consumed-token", Instant.now().plus(30, ChronoUnit.MINUTES));
        token.setConsumedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByToken("consumed-token")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.validateToken("consumed-token"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("already been used"));
    }

    @Test
    void resetPasswordSucceeds() {
        PasswordResetToken token = new PasswordResetToken(testUser, "reset-token", Instant.now().plus(30, ChronoUnit.MINUTES));
        when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecret123")).thenReturn("$2a$10$newhashedpassword");

        PasswordResetResponse response = service.resetPassword("reset-token", "newSecret123");

        assertTrue(response.isSuccess());
        assertEquals("$2a$10$newhashedpassword", testUser.getPassword());
        assertTrue(token.isConsumed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(token);
        verify(refreshTokenRepository).revokeAllByUser(testUser);
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword("token", "12345"));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("at least 6 characters"));
    }
}
