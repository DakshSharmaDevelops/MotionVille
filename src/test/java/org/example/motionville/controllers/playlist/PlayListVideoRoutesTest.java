package org.example.motionville.controllers.playlist;

import org.example.motionville.entity.video.Video;

import org.example.motionville.dto.playlist.PlayListCreateRequest;
import org.example.motionville.dto.playlist.PlayListResponse;
import org.example.motionville.dto.playlist.PlayListUpdateRequest;
import org.example.motionville.dto.playlist.PlayListVideoResponse;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.services.playlist.PlayListService;
import org.example.motionville.services.playlist.PlayListVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayListVideoRoutesTest {

    private StubPlayListVideoService videoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        videoService = new StubPlayListVideoService();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PlayListsController(new StubPlayListService(), videoService),
                new PlayListVideoController(videoService)).build();
    }

    @Test
    void addsVideoAtExactUrl() throws Exception {
        mockMvc.perform(post("/api/playlists/5/videos/12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.videoId").value(12))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    void removesVideoAtExactUrl() throws Exception {
        mockMvc.perform(delete("/api/playlists/5/videos/12"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listsVideosAtExactUrlInOrder() throws Exception {
        mockMvc.perform(get("/api/playlists/5/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].videoId").value(12))
                .andExpect(jsonPath("$[0].position").value(0));
    }

    @Test
    void reordersVideosAtExactUrl() throws Exception {
        mockMvc.perform(put("/api/playlists/5/videos/reorder")
                        .contentType(APPLICATION_JSON)
                        .content("{\"videoIds\":[13,12]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].videoId").value(13))
                .andExpect(jsonPath("$[0].position").value(0));
    }

    private static class StubPlayListService implements PlayListService {
        @Override public List<PlayListResponse> getAllPlayList(Long userId) { return List.of(); }
        @Override public PlayListResponse getPlayList(Long playListId) { return new PlayListResponse(); }
        @Override public PlayListResponse savePlayList(PlayListCreateRequest request) { return new PlayListResponse(); }
        @Override public PlayListResponse updatePlayList(Long playListId, PlayListUpdateRequest request) {
            return new PlayListResponse();
        }
        @Override public void deletePlayList(Long playListId) { }
    }

    private static class StubPlayListVideoService implements PlayListVideoService {
        @Override
        public PlayListVideoResponse addVideo(Long playListId, Long videoId) {
            return response(videoId, 0);
        }

        @Override public void removeVideo(Long playListId, Long videoId) { }

        @Override
        public List<PlayListVideoResponse> listVideos(Long playListId) {
            return List.of(response(12L, 0), response(13L, 1));
        }

        @Override
        public List<PlayListVideoResponse> reorderVideos(Long playListId, List<Long> videoIds) {
            return List.of(response(13L, 0), response(12L, 1));
        }

        private PlayListVideoResponse response(Long videoId, int position) {
            return new PlayListVideoResponse(videoId, "Video " + videoId, null, null,
                    60, VideoVisibility.PUBLIC, VideoProcessingStatus.READY,
                    position, Instant.parse("2026-01-01T00:00:00Z"));
        }
    }
}
