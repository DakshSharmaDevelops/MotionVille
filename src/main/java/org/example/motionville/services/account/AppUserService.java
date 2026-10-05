package org.example.motionville.services.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.motionville.dto.account.UserCreateRequest;
import org.example.motionville.dto.account.UserResponse;
import org.example.motionville.dto.account.UserUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.RefreshTokenRepository;
import org.example.motionville.repo.account.RegistrationOtpRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AppUserService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailValidatorService emailValidatorService;
    private final EmailVerificationService emailVerificationService;

    @Autowired(required = false)
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired(required = false)
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired(required = false)
    private RegistrationOtpRepository registrationOtpRepository;

    public AppUserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            EmailValidatorService emailValidatorService,
            EmailVerificationService emailVerificationService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailValidatorService = emailValidatorService;
        this.emailVerificationService = emailVerificationService;
    }

    public UserResponse createUser(UserCreateRequest request) {

        // Anti-account-enumeration: generic conflict error regardless of whether username or email was the duplicate
        if (appUserRepository.existsByUsername(request.getUsername())
                || appUserRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this username or email already exists"
            );
        }

        // Validate MX records before persisting
        emailValidatorService.validateEmailDomainMx(request.getEmail());

        AppUser user = new AppUser();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        user.setRole("USER");
        user.setDisplayName(request.getDisplayName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setEmailVerified(false);

        AppUser savedUser = appUserRepository.save(user);

        // Generate and dispatch verification token email
        emailVerificationService.createAndSendVerificationToken(savedUser);

        return convertToResponse(savedUser);
    }


    public UserResponse getUserByUsername(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return convertToResponse(user);
    }

    public UserResponse login(String username, String password) {
        AppUser user = username == null ? null : appUserRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || user.getPassword() == null
                || password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }
        return convertToResponse(user);
    }

    public UserResponse getUserById(Long id) {

        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return convertToResponse(user);
    }

    public List<UserResponse> getAllUsers() {

        List<AppUser> users = appUserRepository.findAll();

        List<UserResponse> responses = new ArrayList<>();

        for (AppUser user : users) {
            responses.add(convertToResponse(user));
        }

        return responses;
    }

    public UserResponse updateUser(
            Long id,
            UserUpdateRequest request) {

        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );


        if (appUserRepository.existsByUsernameAndIdNot(
                request.getUsername(),
                id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }


        if (appUserRepository.existsByEmailAndIdNot(
                request.getEmail(),
                id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this username or email already exists"
            );
        }

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.getEmail().trim());
        if (emailChanged) {
            emailValidatorService.validateEmailDomainMx(request.getEmail());
            user.setEmailVerified(false);
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setAvatarUrl(request.getAvatarUrl());

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encodedPassword);
        }


        AppUser updatedUser = appUserRepository.save(user);

        if (emailChanged) {
            emailVerificationService.createAndSendVerificationToken(updatedUser);
        }

        return convertToResponse(updatedUser);
    }

    public void resetPasswordForUser(Long id, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setUpdatedAt(Instant.now());
        appUserRepository.save(user);

        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllByUser(user);
        }
    }

    public void deleteUser(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (entityManager != null) {
            entityManager.createNativeQuery("DELETE FROM notifications WHERE recipient_id = :userId OR actor_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM reports WHERE reporter_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM comment_reactions WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM video_reactions WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM watch_history WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM video_views WHERE viewer_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM refresh_tokens WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_reset_tokens WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM email_verification_tokens WHERE user_id = :userId").setParameter("userId", id).executeUpdate();
            entityManager.clear();
        }

        if (registrationOtpRepository != null) {
            registrationOtpRepository.deleteAllByEmail(user.getEmail());
        }

        appUserRepository.delete(user);
    }

    private UserResponse convertToResponse(AppUser user) {

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRole(user.getRole() == null ? "USER" : user.getRole());
        response.setEmailVerified(user.isEmailVerified());

        return response;
    }
}
