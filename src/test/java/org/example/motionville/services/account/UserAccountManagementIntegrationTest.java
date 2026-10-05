package org.example.motionville.services.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.motionville.dto.account.ChangePasswordRequest;
import org.example.motionville.dto.account.LoginRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.repo.account.AppUserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class UserAccountManagementIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private org.example.motionville.repo.channel.ChannelRepository channelRepository;

    @Autowired
    private org.example.motionville.repo.video.VideoRepository videoRepository;

    @Autowired
    private org.example.motionville.repo.comment.CommentRepository commentRepository;

    @Autowired
    private org.example.motionville.repo.notification.NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private AppUser user;
    private Cookie authCookie;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        user = new AppUser();
        user.setUsername("manageuser");
        user.setDisplayName("Manage User");
        user.setEmail("manageuser@example.com");
        user.setPassword(passwordEncoder.encode("OriginalPass123"));
        user.setEmailVerified(true);
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        authCookie = new Cookie("access_token", accessToken);
    }

    @Test
    void userCanResetPasswordWhileLoggedIn() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(null, "NewPassword999");

        mockMvc.perform(post("/api/users/" + user.getId() + "/reset-password")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password successfully reset"));

        // Old password fails
        LoginRequest oldLogin = new LoginRequest("manageuser", "OriginalPass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // New password works
        LoginRequest newLogin = new LoginRequest("manageuser", "NewPassword999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manageuser"));
    }

    @Test
    void userCanDeleteAccount() throws Exception {
        mockMvc.perform(delete("/api/users/" + user.getId())
                        .cookie(authCookie)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.findById(user.getId()).isPresent());

        // Login fails because account was deleted
        LoginRequest login = new LoginRequest("manageuser", "OriginalPass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanDeleteAccountWithCommentsAndNotifications() throws Exception {
        // 1. Create a channel for user
        org.example.motionville.entity.channel.Channel channel = new org.example.motionville.entity.channel.Channel();
        channel.setName("Test Channel");
        channel.setHandle("testchannel");
        channel.setOwner(user);
        channel = channelRepository.save(channel);

        // 2. Create a video in channel
        org.example.motionville.entity.video.Video video = new org.example.motionville.entity.video.Video();
        video.setTitle("Sample Video");
        video.setChannel(channel);
        video.setVisibility(org.example.motionville.entity.video.enums.VideoVisibility.PUBLIC);
        video.setProcessingStatus(org.example.motionville.entity.video.enums.VideoProcessingStatus.READY);
        video = videoRepository.save(video);

        // 3. User comments on video
        org.example.motionville.entity.comment.Comment comment = new org.example.motionville.entity.comment.Comment();
        comment.setBody("Hello world!");
        comment.setAuthor(user);
        comment.setVideo(video);
        comment = commentRepository.save(comment);

        // 4. Notification references this comment (the exact constraint that caused the failure)
        org.example.motionville.entity.notification.Notification notification =
                new org.example.motionville.entity.notification.Notification();
        notification.setRecipient(user);
        notification.setActor(user);
        notification.setVideo(video);
        notification.setComment(comment);
        notification.setType(org.example.motionville.entity.notification.enums.NotificationType.NEW_COMMENT);
        notification.setMessage("New comment notification");
        notificationRepository.save(notification);

        // 5. Delete user should cleanly cascade and succeed
        mockMvc.perform(delete("/api/users/" + user.getId())
                        .cookie(authCookie)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.findById(user.getId()).isPresent());
        assertFalse(channelRepository.findById(channel.getChannelId()).isPresent());
        assertFalse(videoRepository.findById(video.getVideoId()).isPresent());
        assertFalse(commentRepository.findById(comment.getId()).isPresent());
    }
}
