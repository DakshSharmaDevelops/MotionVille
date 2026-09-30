package org.example.motionville.controllers;

import org.example.motionville.dto.CommentCreateRequest;
import org.example.motionville.dto.CommentRequest;
import org.example.motionville.dto.CommentResponse;
import org.example.motionville.services.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentsController(new StubCommentService()))
                .setValidator(validator)
                .build();
    }

    @Test
    void getsCommentsForVideo() throws Exception {
        mockMvc.perform(get("/api/videos/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].videoId").value(10));
    }

    @Test
    void createsComment() throws Exception {
        mockMvc.perform(post("/api/videos/10/comments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"authorId\":3,\"body\":\"Hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hello"))
                .andExpect(jsonPath("$.authorId").value(3));
    }

    @Test
    void rejectsBlankComment() throws Exception {
        mockMvc.perform(post("/api/videos/10/comments")
                        .contentType(APPLICATION_JSON)
                        .content("{\"authorId\":3,\"body\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesComment() throws Exception {
        mockMvc.perform(put("/api/comments/7")
                        .contentType(APPLICATION_JSON)
                        .content("{\"body\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Updated"));
    }

    @Test
    void deletesComment() throws Exception {
        mockMvc.perform(delete("/api/comments/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getsReplies() throws Exception {
        mockMvc.perform(get("/api/comments/7/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentCommentId").value(7));
    }

    @Test
    void createsReply() throws Exception {
        mockMvc.perform(post("/api/comments/7/replies")
                        .contentType(APPLICATION_JSON)
                        .content("{\"authorId\":3,\"body\":\"Reply\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCommentId").value(7))
                .andExpect(jsonPath("$.body").value("Reply"));
    }

    private static class StubCommentService implements CommentService {
        @Override
        public List<CommentResponse> findByVideoId(Long videoId) {
            return List.of(response(7L, videoId, null, "Hello"));
        }

        @Override
        public List<CommentResponse> findReplies(Long commentId) {
            return List.of(response(8L, 10L, commentId, "Reply"));
        }

        @Override
        public CommentResponse createComment(Long videoId, CommentCreateRequest request) {
            return response(7L, videoId, null, request.getBody());
        }

        @Override
        public CommentResponse createReply(Long commentId, CommentCreateRequest request) {
            return response(8L, 10L, commentId, request.getBody());
        }

        @Override
        public CommentResponse updateComment(Long id, CommentRequest request) {
            return response(id, 10L, null, request.getBody());
        }

        @Override
        public void deleteComment(Long id) {
            // No persistence in controller-focused tests.
        }

        private CommentResponse response(Long id, Long videoId, Long parentId, String body) {
            CommentResponse response = new CommentResponse();
            response.setId(id);
            response.setVideoId(videoId);
            response.setAuthorId(3L);
            response.setAuthorDisplayName("Test Author");
            response.setParentCommentId(parentId);
            response.setBody(body);
            response.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            response.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            return response;
        }
    }
}
