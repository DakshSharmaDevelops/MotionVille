package org.example.motionville.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.PlayListReorderRequest;
import org.example.motionville.dto.PlayListVideoResponse;
import org.example.motionville.services.PlayListVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists/{playlistId}/videos")
public class PlayListVideoController {

    private final PlayListVideoService playListVideoService;

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isPlaylistOwner(#playlistId, authentication)")
    @PostMapping("/{videoId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayListVideoResponse addVideo(@PathVariable Long playlistId,
                                          @PathVariable Long videoId) {
        return playListVideoService.addVideo(playlistId, videoId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isPlaylistOwner(#playlistId, authentication)")
    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVideo(@PathVariable Long playlistId,
                            @PathVariable Long videoId) {
        playListVideoService.removeVideo(playlistId, videoId);
    }

    @PreAuthorize("@authorizationService.canViewPlaylist(#playlistId, authentication)")
    @GetMapping
    public List<PlayListVideoResponse> getPlayListVideos(@PathVariable Long playlistId) {
        return playListVideoService.listVideos(playlistId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isPlaylistOwner(#playlistId, authentication)")
    @PutMapping("/reorder")
    public List<PlayListVideoResponse> reorderVideos(@PathVariable Long playlistId,
                                                     @Valid @RequestBody PlayListReorderRequest request) {
        return playListVideoService.reorderVideos(playlistId, request.getVideoIds());
    }
}