package org.example.motionville;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import org.example.motionville.video.LocalMediaStorage;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.example.motionville.account.AccountService;
import org.example.motionville.account.Role;
import org.example.motionville.account.UserAccount;
import org.example.motionville.account.UserAccountRepository;
import org.example.motionville.account.web.RegisterForm;
import org.example.motionville.channel.Channel;
import org.example.motionville.channel.ChannelService;
import org.example.motionville.channel.web.ChannelForm;
import org.example.motionville.engagement.EngagementService;
import org.example.motionville.video.Video;
import org.example.motionville.video.VideoService;
import org.example.motionville.video.VideoVisibility;
import org.example.motionville.video.web.VideoUploadForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MotionVilleApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accounts;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChannelService channels;

    @Autowired
    private VideoService videos;

    @Autowired
    private LocalMediaStorage mediaStorage;

    @Autowired
    private EngagementService engagement;

    private final List<String> uploadedMedia = new ArrayList<>();

    @AfterEach
    void removeTestUploads() {
        uploadedMedia.forEach(mediaStorage::delete);
        uploadedMedia.clear();
    }

    @Test
    void homePageRendersForAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your video feed")));
        mockMvc.perform(get("/channels/create"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void homeFeedWorksWithoutThirdPartyConfiguration() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your video feed")));
    }

    @Test
    void servesLazyVideoThumbnailPreviewScript() throws Exception {
        mockMvc.perform(get("/js/video-thumbnails.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("loadedmetadata")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IntersectionObserver")));
    }

    @Test
    void subscriptionFeedRequiresLoginAndRendersForSignedInUsers() throws Exception {
        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount viewer = accounts.register(new RegisterForm(
                suffix + "@example.test", "viewer-" + suffix, "Viewer", "long-enough-password-123"));
        UserAccount creator = accounts.register(new RegisterForm(
                "creator-" + suffix + "@example.test", "creator-" + suffix, "Creator", "long-enough-password-123"));
        Channel channel = channels.create(creator.getId(), new ChannelForm("Followed " + suffix, "A followed channel"));
        engagement.setSubscription(channel.getId(), viewer.getId(), true);
        var login = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin()
                        .user("username", suffix + "@example.test")
                        .password("long-enough-password-123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/subscriptions").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Subscriptions")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Followed " + suffix)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No new videos yet")));
    }

    @Test
    void registrationStoresOnlyEncodedPassword() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String rawPassword = "long-enough-password-123";
        UserAccount account = accounts.register(new RegisterForm(
                suffix + "@example.test", "user-" + suffix, "New Creator", rawPassword));

        UserAccount stored = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(Role.VIEWER);
        assertThat(stored.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, stored.getPasswordHash())).isTrue();
    }

    @Test
    void creatorCanUploadAndEngageWithVideo() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount creator = accounts.register(new RegisterForm(
                suffix + "@example.test", "creator-" + suffix, "Creator", "long-enough-password-123"));
        Channel channel = channels.create(creator.getId(), new ChannelForm("Channel " + suffix, "A test channel"));

        VideoUploadForm form = new VideoUploadForm();
        form.setTitle("A test video");
        form.setDescription("Video description");
        form.setCategory("Education");
        form.setVisibility(VideoVisibility.PUBLIC);
        form.setFile(new MockMultipartFile(
                "file", "test.mp4", "video/mp4",
                new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 0, 0, 0, 0}));
        Video video = videos.upload(creator.getId(), form);
        uploadedMedia.add(video.getStorageKey());

        engagement.addComment(video.getId(), creator.getId(), "A useful comment");
        engagement.setLike(video.getId(), creator.getId(), true);
        assertThat(engagement.countLikes(video.getId())).isEqualTo(1);

        mockMvc.perform(get("/videos/{id}", video.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A test video")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A useful comment")));
        mockMvc.perform(get("/").param("q", "test").param("category", "education"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A test video")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("thumbnail-preview")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/media/" + video.getStorageKey())));
        mockMvc.perform(get("/").param("source", "local").param("feed", "trending"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A test video")));
        mockMvc.perform(get("/channels/{id}", channel.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Channel " + suffix)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("video-thumbnails.js")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/videos/{id}/like", video.getId()))
                .andExpect(status().isForbidden());
        var login = mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin()
                        .user("username", creator.getEmail())
                        .password("long-enough-password-123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/videos/{id}/edit", video.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit video details")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/videos/{id}/like", video.getId())
                        .session(session)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(engagement.countLikes(video.getId())).isEqualTo(1);
        var rangeResponse = mockMvc.perform(get("/media/{key}", video.getStorageKey())
                        .header("Range", "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-3/12"))
                .andReturn();
        mockMvc.perform(asyncDispatch(rangeResponse))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes(new byte[] {0, 0, 0, 12}));
        assertThat(channels.get(channel.getId()).getOwner().getRole()).isEqualTo(Role.CREATOR);
    }
}
