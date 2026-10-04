package org.example.motionville.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.example.motionville.dto.account.LoginRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.RefreshToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.account.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JwtAuthenticationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        Instant now = Instant.now();
        testUser = new AppUser();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode("SecretPass123!"));
        testUser.setDisplayName("Test User");
        testUser.setRole("USER");
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);
        testUser = userRepository.save(testUser);
    }

    @Test
    void testLoginSetsAccessAndRefreshCookies() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("SecretPass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andReturn();

        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        assertNotNull(cookies);
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("access_token=") && c.contains("HttpOnly")));
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("refresh_token=") && c.contains("HttpOnly") && c.contains("Path=/api/auth")));
    }

    @Test
    void testProtectedMeEndpointWithValidJwtCookie() throws Exception {
        String token = jwtService.generateAccessToken(testUser);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("access_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@example.com"));
    }

    @Test
    void testProtectedMeEndpointWithBearerHeader() throws Exception {
        String token = jwtService.generateAccessToken(testUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void testProtectedMeEndpointFailsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedMeEndpointFailsWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("access_token", "invalid.jwt.token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshTokenRotationSuccess() throws Exception {
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshToken.getToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertNotNull(setCookies);
        assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("access_token=")));
        assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("refresh_token=")));

        // Verify old token was revoked
        RefreshToken oldToken = refreshTokenRepository.findByToken(refreshToken.getToken()).orElseThrow();
        assertTrue(oldToken.isRevoked(), "Old refresh token should be revoked after rotation");
    }

    @Test
    void testRefreshTokenReuseCompromiseDetection() throws Exception {
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser);

        // First rotation succeeds
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshToken.getToken())))
                .andExpect(status().isOk());

        // Attempting to reuse the revoked token triggers compromise detection
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshToken.getToken())))
                .andExpect(status().isUnauthorized());

        // All tokens for the user must now be revoked
        List<RefreshToken> allTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(testUser.getId()))
                .toList();
        assertFalse(allTokens.isEmpty());
        assertTrue(allTokens.stream().allMatch(RefreshToken::isRevoked),
                "All refresh tokens for compromised user must be revoked");
    }

    @Test
    void testLogoutRevokesRefreshTokenAndClearsCookies() throws Exception {
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser);

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshToken.getToken())))
                .andExpect(status().isNoContent())
                .andReturn();

        // Verify token revoked
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken.getToken()).orElseThrow();
        assertTrue(stored.isRevoked());

        // Verify clearance cookies
        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("access_token=") && c.contains("Max-Age=0")));
        assertTrue(cookies.stream().anyMatch(c -> c.startsWith("refresh_token=") && c.contains("Max-Age=0")));
    }
}
