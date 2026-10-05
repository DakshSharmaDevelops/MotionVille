package org.example.motionville.controllers.engagement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.engagement.WatchHistoryResponse;
import org.example.motionville.dto.engagement.WatchHistoryUpdateRequest;
import org.example.motionville.security.AuthorizationService;
import org.example.motionville.services.engagement.WatchHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final AuthorizationService authorizationService;

    @PostMapping("/videos/{videoId}/history")
    public WatchHistoryResponse recordProgress(
            @PathVariable Long videoId,
            @Valid @RequestBody WatchHistoryUpdateRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            WatchHistoryResponse response = new WatchHistoryResponse();
            response.setVideoId(videoId);
            response.setLastPositionSeconds(request.getLastPositionSeconds());
            response.setLastWatchedAt(java.time.Instant.now());
            return response;
        }

        if (request.getUserId() == null || (!authorizationService.isAdmin(authentication) && !authorizationService.isUserOwner(request.getUserId(), authentication))) {
            Long currentUserId = authorizationService.getAuthenticatedUserId(authentication);
            if (currentUserId != null) {
                request.setUserId(currentUserId);
            }
        }
        return watchHistoryService.recordProgress(videoId, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping("/users/{userId}/history")
    public List<WatchHistoryResponse> getHistory(@PathVariable Long userId) {
        return watchHistoryService.getHistory(userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @DeleteMapping("/users/{userId}/history/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHistoryItem(@PathVariable Long userId, @PathVariable Long videoId) {
        watchHistoryService.removeHistoryItem(userId, videoId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @DeleteMapping("/users/{userId}/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(@PathVariable Long userId) {
        watchHistoryService.clearHistory(userId);
    }

}