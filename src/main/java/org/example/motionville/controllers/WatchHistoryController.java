package org.example.motionville.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.WatchHistoryResponse;
import org.example.motionville.dto.WatchHistoryUpdateRequest;
import org.example.motionville.services.WatchHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @PostMapping("/videos/{videoId}/history")
    public WatchHistoryResponse recordProgress(
            @PathVariable Long videoId,
            @Valid @RequestBody WatchHistoryUpdateRequest request) {
        return watchHistoryService.recordProgress(videoId, request);
    }

    @GetMapping("/users/{userId}/history")
    public List<WatchHistoryResponse> getHistory(@PathVariable Long userId) {
        return watchHistoryService.getHistory(userId);
    }

    @DeleteMapping("/users/{userId}/history/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHistoryItem(@PathVariable Long userId, @PathVariable Long videoId) {
        watchHistoryService.removeHistoryItem(userId, videoId);
    }

    @DeleteMapping("/users/{userId}/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(@PathVariable Long userId) {
        watchHistoryService.clearHistory(userId);
    }

}
