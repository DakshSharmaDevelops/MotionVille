package org.example.motionville.services.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.motionville.dto.account.LoginRequest;
import org.example.motionville.dto.account.ResendVerificationRequest;
import org.example.motionville.dto.account.UserCreateRequest;
import org.example.motionville.dto.account.VerifyEmailRequest;
import org.example.motionville.dto.channel.ChannelCreateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.EmailVerificationToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.EmailVerificationTokenRepository;
import org.example.motionville.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "motionville.mail.enabled=false",
        "motionville.mail.mx-validation.enabled=false"
})
@Transactional
class EmailVerificationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private org.example.motionville.repo.account.RegistrationOtpRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void registrationCreatesUnverifiedUserAndDispatchesToken() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("alice_verify");
        request.setEmail("alice_verify@example.com");
        request.setPassword("StrongPassword123!");
        request.setDisplayName("Alice Verify");

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice_verify"))
                .andExpect(jsonPath("$.emailVerified").value(false));

        AppUser user = userRepository.findByUsername("alice_verify").orElseThrow();
        assertFalse(user.isEmailVerified());

        EmailVerificationToken token = tokenRepository.findTopByUserOrderByCreatedAtDesc(user).orElseThrow();
        assertNotNull(token.getToken());
        assertFalse(token.isExpired());
    }

    @Test
    void unverifiedUserCanLogin_butCannotCreateChannelUntilVerified() throws Exception {
        AppUser unverifiedUser = new AppUser();
        unverifiedUser.setUsername("charlie_unverified");
        unverifiedUser.setEmail("charlie@example.com");
        unverifiedUser.setPassword(passwordEncoder.encode("SecretPass123"));
        unverifiedUser.setDisplayName("Charlie");
        unverifiedUser.setEmailVerified(false);
        unverifiedUser = userRepository.save(unverifiedUser);

        // 1. Unverified user can log in
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("charlie_unverified");
        loginRequest.setPassword("SecretPass123");

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andReturn();

        String accessToken = jwtService.generateAccessToken(unverifiedUser);
        Cookie accessCookie = new Cookie("access_token", accessToken);

        // 2. Sensitive operation (create channel) is forbidden for unverified user
        ChannelCreateRequest channelRequest = new ChannelCreateRequest();
        channelRequest.setOwnerId(unverifiedUser.getId());
        channelRequest.setName("Charlie Channel");
        channelRequest.setHandle("@charlie");
        channelRequest.setDescription("Description");

        mockMvc.perform(post("/api/channels")
                        .with(csrf())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(channelRequest)))
                .andExpect(status().isForbidden());

        // 3. User verifies email
        EmailVerificationToken token = new EmailVerificationToken(
                unverifiedUser,
                "charlie-token-123",
                java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
        );
        tokenRepository.save(token);

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest("charlie-token-123");
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        AppUser refreshedUser = userRepository.findById(unverifiedUser.getId()).orElseThrow();
        assertTrue(refreshedUser.isEmailVerified());

        // 4. Now sensitive operation (create channel) succeeds
        mockMvc.perform(post("/api/channels")
                        .with(csrf())
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(channelRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Charlie Channel"));
    }

    @Test
    void resendVerificationReturnsGenericResponseAndEnforcesCooldown() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("david_resend");
        user.setEmail("david@example.com");
        user.setPassword(passwordEncoder.encode("Pass123456"));
        user.setDisplayName("David");
        user.setEmailVerified(false);
        user = userRepository.save(user);

        ResendVerificationRequest request = new ResendVerificationRequest("david@example.com");

        // First attempt -> generic 200 OK
        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email is associated with an unverified account, a verification link has been sent."));

        // Immediate second attempt -> 429 Too Many Requests (cooldown)
        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void userCanVerifyAccountViaOtpOrCheckStatus() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("dual_user");
        request.setEmail("dual_user@example.com");
        request.setPassword("StrongPassword123!");
        request.setDisplayName("Dual User");

        // 1. Create account
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerified").value(false));

        // 2. Both token and OTP exist
        AppUser user = userRepository.findByUsername("dual_user").orElseThrow();
        assertFalse(user.isEmailVerified());

        var token = tokenRepository.findTopByUserOrderByCreatedAtDesc(user).orElseThrow();
        assertNotNull(token.getToken());

        var otp = otpRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc("dual_user@example.com").orElseThrow();
        assertNotNull(otp.getOtp());
        assertEquals(6, otp.getOtp().length());

        // 3. Status check returns unverified initially
        mockMvc.perform(get("/api/auth/verification-status")
                        .param("email", "dual_user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));

        // 4. Verify via OTP
        org.example.motionville.dto.account.VerifyOtpRequest otpReq =
                new org.example.motionville.dto.account.VerifyOtpRequest("dual_user@example.com", otp.getOtp());

        mockMvc.perform(post("/api/auth/verify-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        // 5. User is now verified and status check returns true
        AppUser verifiedUser = userRepository.findByUsername("dual_user").orElseThrow();
        assertTrue(verifiedUser.isEmailVerified());

        mockMvc.perform(get("/api/auth/verification-status")
                        .param("email", "dual_user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void resendVerificationNonExistentEmailReturnsGenericResponse() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("doesnotexist@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email is associated with an unverified account, a verification link has been sent."));
    }
}
