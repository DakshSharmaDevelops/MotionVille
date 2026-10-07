package org.example.motionville.services.account;

import jakarta.persistence.EntityManager;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.RefreshTokenRepository;
import org.example.motionville.repo.account.RegistrationOtpRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserServiceTest {
    private EntityManager entityManager;
    private AppUserRepository repository;
    private PasswordEncoder passwordEncoder;
    private EmailValidatorService emailValidatorService;
    private EmailVerificationService emailVerificationService;
    private RefreshTokenRepository refreshTokenRepository;
    private WatchHistoryRepository watchHistoryRepository;
    private RegistrationOtpRepository registrationOtpRepository;
    private AppUserService service;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        repository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailValidatorService = mock(EmailValidatorService.class);
        emailVerificationService = mock(EmailVerificationService.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        watchHistoryRepository = mock(WatchHistoryRepository.class);
        registrationOtpRepository = mock(RegistrationOtpRepository.class);
        service = new AppUserService(
                entityManager,
                repository,
                passwordEncoder,
                emailValidatorService,
                emailVerificationService,
                refreshTokenRepository,
                watchHistoryRepository,
                registrationOtpRepository
        );
    }

    @Test
    void loginReturnsUserForMatchingEncodedPassword() {
        AppUser user = new AppUser();
        user.setId(17L);
        user.setUsername("creator");
        user.setPassword("$2a$encoded");
        when(repository.findByUsername("creator")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "$2a$encoded")).thenReturn(true);

        var result = service.login(" creator ", "secret");

        assertEquals(17L, result.getId());
    }

    @Test
    void loginRejectsUnknownUserAndIncorrectPassword() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());
        AppUser user = new AppUser();
        user.setUsername("creator");
        user.setPassword("$2a$encoded");
        when(repository.findByUsername("creator")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        var missingUser = assertThrows(
                ResponseStatusException.class,
                () -> service.login("unknown", "secret")
        );
        var badPassword = assertThrows(
                ResponseStatusException.class,
                () -> service.login("creator", "wrong")
        );

        assertEquals(401, missingUser.getStatusCode().value());
        assertEquals(401, badPassword.getStatusCode().value());
    }
}
