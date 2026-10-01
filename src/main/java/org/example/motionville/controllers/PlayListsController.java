package org.example.motionville.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.PlayListCreateRequest;
import org.example.motionville.dto.PlayListReorderRequest;
import org.example.motionville.dto.PlayListResponse;
import org.example.motionville.dto.PlayListUpdateRequest;
import org.example.motionville.dto.PlayListVideoResponse;
import org.example.motionville.services.PlayListService;
import org.example.motionville.services.PlayListVideoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlayListsController {

    private final PlayListService playListService;
    private final PlayListVideoService playListVideoService;

    @GetMapping
    public List<PlayListResponse> getPlayLists(@RequestParam Long userId) {
        return playListService.getAllPlayList(userId);
    }

    @GetMapping("/{playListId}")
    public PlayListResponse getPlayList(@PathVariable Long playListId) {
        return playListService.getPlayList(playListId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayListResponse savePlayList(@Valid @RequestBody PlayListCreateRequest request) {
        return playListService.savePlayList(request);
    }

    @PutMapping("/{playListId}")
    public PlayListResponse updatePlayList(@PathVariable Long playListId,
                                           @Valid @RequestBody PlayListUpdateRequest request) {
        return playListService.updatePlayList(playListId, request);
    }

    @DeleteMapping("/{playListId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayList(@PathVariable Long playListId) {
        playListService.deletePlayList(playListId);
    }

}
