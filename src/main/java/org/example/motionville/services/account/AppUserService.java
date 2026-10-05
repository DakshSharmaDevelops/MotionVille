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
            // 1. Delete notifications touching this user (as recipient, actor, or on user's comments/videos)
            entityManager.createNativeQuery("""
                DELETE FROM notifications
                WHERE recipient_id = :userId
                   OR actor_id = :userId
                   OR comment_id IN (SELECT id FROM comments WHERE author_id = :userId)
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 2. Delete reports touching this user, their comments, or their channel videos
            entityManager.createNativeQuery("""
                DELETE FROM reports
                WHERE reporter_id = :userId
                   OR comment_id IN (SELECT id FROM comments WHERE author_id = :userId)
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 3. Delete comment reactions for user or on user's comments
            entityManager.createNativeQuery("""
                DELETE FROM comment_reactions
                WHERE user_id = :userId
                   OR comment_id IN (SELECT id FROM comments WHERE author_id = :userId)
                   OR comment_id IN (SELECT co.id FROM comments co JOIN videos v ON co.video_id = v.id JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 4. Detach parent/child comment relationships so self/child references don't fail
            entityManager.createNativeQuery("""
                UPDATE comments SET parent_comment_id = NULL
                WHERE author_id = :userId
                   OR parent_comment_id IN (SELECT id FROM comments WHERE author_id = :userId)
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 5. Delete comments written by user, or written on user's videos
            entityManager.createNativeQuery("""
                DELETE FROM comments
                WHERE author_id = :userId
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 6. Delete video reactions
            entityManager.createNativeQuery("""
                DELETE FROM video_reactions
                WHERE user_id = :userId
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 7. Delete watch history
            entityManager.createNativeQuery("""
                DELETE FROM watch_history
                WHERE user_id = :userId
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 8. Delete video views
            entityManager.createNativeQuery("""
                DELETE FROM video_views
                WHERE viewer_id = :userId
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 9. Delete playlist_videos (in user's playlists, or referencing user's videos)
            entityManager.createNativeQuery("""
                DELETE FROM playlist_videos
                WHERE play_list_id IN (SELECT id FROM playlists WHERE owner_id = :userId)
                   OR video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 10. Delete playlists
            entityManager.createNativeQuery("DELETE FROM playlists WHERE owner_id = :userId")
                    .setParameter("userId", id).executeUpdate();

            // 11. Delete video_tags for user's videos
            entityManager.createNativeQuery("""
                DELETE FROM video_tags
                WHERE video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 12. Delete video_assets for user's videos
            entityManager.createNativeQuery("""
                DELETE FROM video_assets
                WHERE video_id IN (SELECT v.id FROM videos v JOIN channels c ON v.channel_id = c.id WHERE c.owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 13. Delete videos for user's channels
            entityManager.createNativeQuery("""
                DELETE FROM videos
                WHERE channel_id IN (SELECT id FROM channels WHERE owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 14. Delete subscriptions (where user is subscriber or channel is owned by user)
            entityManager.createNativeQuery("""
                DELETE FROM subscriptions
                WHERE subscriber_id = :userId
                   OR channel_id IN (SELECT id FROM channels WHERE owner_id = :userId)
            """).setParameter("userId", id).executeUpdate();

            // 15. Delete channels
            entityManager.createNativeQuery("DELETE FROM channels WHERE owner_id = :userId")
                    .setParameter("userId", id).executeUpdate();

            // 16. Delete tokens
            entityManager.createNativeQuery("DELETE FROM refresh_tokens WHERE user_id = :userId")
                    .setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM password_reset_tokens WHERE user_id = :userId")
                    .setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM email_verification_tokens WHERE user_id = :userId")
                    .setParameter("userId", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM registration_otps WHERE lower(email) = :email")
                    .setParameter("email", user.getEmail().toLowerCase()).executeUpdate();

            // 17. Finally delete the user!
            entityManager.createNativeQuery("DELETE FROM app_user WHERE id = :userId")
                    .setParameter("userId", id).executeUpdate();

            entityManager.clear();
        } else {
            if (refreshTokenRepository != null) {
                refreshTokenRepository.revokeAllByUser(user);
            }
            if (watchHistoryRepository != null) {
                watchHistoryRepository.deleteAllByUser_Id(user.getId());
            }
            if (registrationOtpRepository != null) {
                registrationOtpRepository.deleteAllByEmail(user.getEmail());
            }

            appUserRepository.delete(user);
        }
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
