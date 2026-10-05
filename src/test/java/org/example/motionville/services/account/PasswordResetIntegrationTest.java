package org.example.motionville.services.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.motionville.dto.account.ForgotPasswordRequest;
import org.example.motionville.dto.account.LoginRequest;
import org.example.motionville.dto.account.ResetPasswordRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.PasswordResetToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.PasswordResetTokenRepository;
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

import static org.junit.jupiter.api.Assertions.*;
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
class PasswordResetIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        tokenRepository.deleteAll();
        userRepository.deleteAll();

        user = new AppUser();
        user.setUsername("resetuser");
        user.setDisplayName("Reset User");
        user.setEmail("resetuser@motionville.internal");
        user.setPassword(passwordEncoder.encode("OldPassword123"));
        user.setEmailVerified(true);
        user = userRepository.save(user);
    }

    @Test
    void completePasswordResetLifecycle() throws Exception {
        // 1. Request password reset
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("resetuser@motionville.internal");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Verify token was generated in database
        PasswordResetToken token = tokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new AssertionError("Token should have been generated"));
        assertNotNull(token.getToken());
        assertTrue(token.isValid());

        // 3. Validate token endpoint
        mockMvc.perform(get("/api/auth/reset-password/validate")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Submit new password
        ResetPasswordRequest resetReq = new ResetPasswordRequest(token.getToken(), "BrandNewPassword456");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Verify token is consumed
        PasswordResetToken updatedToken = tokenRepository.findByToken(token.getToken()).orElseThrow();
        assertTrue(updatedToken.isConsumed());

        // 6. Old password no longer works
        LoginRequest oldLogin = new LoginRequest("resetuser", "OldPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // 7. New password works
        LoginRequest newLogin = new LoginRequest("resetuser", "BrandNewPassword456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("resetuser"));
    }
}
