package org.example.motionville.controllers.video;

import org.example.motionville.dto.engagement.VideoReactionResponse;
import org.example.motionville.dto.engagement.VideoReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.services.engagement.VideoReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VideoReactionControllerTest {

    private StubVideoReactionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new StubVideoReactionService();
        mockMvc = MockMvcBuilders.standaloneSetup(new VideoReactionController(service)).build();
    }

    @Test
    void getsReactionSummary() throws Exception {
        mockMvc.perform(get("/api/videos/10/reaction-summary").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(4))
                .andExpect(jsonPath("$.dislikeCount").value(1))
                .andExpect(jsonPath("$.userReaction").value("LIKE"));
    }

    @Test
    void likesVideoForUser() throws Exception {
        mockMvc.perform(post("/api/videos/10/like").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(10))
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.reaction").value("LIKE"));
    }

    @Test
    void dislikesVideoForUser() throws Exception {
        mockMvc.perform(post("/api/videos/10/dislike").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reaction").value("DISLIKE"));
    }

    @Test
    void requiresUserIdForLike() throws Exception {
        mockMvc.perform(post("/api/videos/10/like"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesReaction() throws Exception {
        mockMvc.perform(delete("/api/videos/10/reaction").param("userId", "3"))
                .andExpect(status().isOk());
    }

    private static class StubVideoReactionService implements VideoReactionService {
        @Override
        public VideoReactionSummary getVideoReactionSummary(Long videoId, Long userId) {
            return new VideoReactionSummary(4, 1, userId == null ? null : ReactionType.LIKE);
        }

        @Override
        public VideoReactionResponse setReaction(Long videoId, Long userId, ReactionType type) {
            return new VideoReactionResponse(videoId, userId, type);
        }

        @Override
        public void deleteReaction(Long videoId, Long userId) {
            // No persistence in controller-focused tests.
        }
    }
}
