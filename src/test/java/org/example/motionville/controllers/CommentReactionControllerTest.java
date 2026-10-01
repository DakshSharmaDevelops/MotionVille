package org.example.motionville.controllers;

import org.example.motionville.dto.CommentReactionResponse;
import org.example.motionville.dto.CommentReactionSummary;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.services.CommentReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentReactionControllerTest {

    private StubCommentReactionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new StubCommentReactionService();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentReactionController(service)).build();
    }

    @Test
    void getsSummaryWithUserReaction() throws Exception {
        mockMvc.perform(get("/api/comments/12/reaction-summary").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.dislikeCount").value(2))
                .andExpect(jsonPath("$.userReaction").value("LIKE"));
    }

    @Test
    void getsSummaryWithoutUserId() throws Exception {
        mockMvc.perform(get("/api/comments/12/reaction-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.userReaction").doesNotExist());
    }

    @Test
    void likesComment() throws Exception {
        mockMvc.perform(post("/api/comments/12/like").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(12))
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.reactionType").value("LIKE"));
    }

    @Test
    void dislikesComment() throws Exception {
        mockMvc.perform(post("/api/comments/12/dislike").param("userId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reactionType").value("DISLIKE"));
    }

    @Test
    void requiresUserIdForLike() throws Exception {
        mockMvc.perform(post("/api/comments/12/like"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesCommentReaction() throws Exception {
        mockMvc.perform(delete("/api/comments/12/reaction").param("userId", "3"))
                .andExpect(status().isOk());
    }

    private static class StubCommentReactionService implements CommentReactionService {
        @Override
        public CommentReactionSummary getCommentReactionSummary(Long commentId, Long userId) {
            return new CommentReactionSummary(5, 2,
                    userId == null ? null : ReactionType.LIKE);
        }

        @Override
        public CommentReactionResponse setCommentReaction(Long commentId, Long userId,
                                                          ReactionType reactionType) {
            return new CommentReactionResponse(commentId, userId, reactionType);
        }

        @Override
        public void deleteCommentReaction(Long commentId, Long userId) {
            // Controller-focused test; persistence is not involved.
        }
    }
}
