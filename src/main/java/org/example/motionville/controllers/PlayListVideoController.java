package org.example.motionville.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.PlayListReorderRequest;
import org.example.motionville.dto.PlayListVideoResponse;
import org.example.motionville.services.PlayListVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists/{playlistId}/videos")
public class PlayListVideoController {

    private final PlayListVideoService playListVideoService;

    @PostMapping("/{videoId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayListVideoResponse addVideo(@PathVariable Long playlistId,
                                          @PathVariable Long videoId) {
        return playListVideoService.addVideo(playlistId, videoId);
    }

    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVideo(@PathVariable Long playlistId,
                            @PathVariable Long videoId) {
        playListVideoService.removeVideo(playlistId, videoId);
    }

    @GetMapping
    public List<PlayListVideoResponse> getPlayListVideos(@PathVariable Long playlistId) {
        return playListVideoService.listVideos(playlistId);
    }

    @PutMapping("/reorder")
    public List<PlayListVideoResponse> reorderVideos(@PathVariable Long playlistId,
                                                     @Valid @RequestBody PlayListReorderRequest request) {
        return playListVideoService.reorderVideos(playlistId, request.getVideoIds());
    }
}
