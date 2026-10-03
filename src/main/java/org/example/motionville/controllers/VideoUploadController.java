package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.VideoPlaybackResponse;
import org.example.motionville.dto.VideoThumbnailResponse;
import org.example.motionville.dto.VideoUploadRequest;
import org.example.motionville.dto.VideoUploadResponse;
import org.example.motionville.services.videoService.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class VideoUploadController {

    private final VideoService videoService;

    public VideoUploadController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isChannelOwner(#request.channelId(), authentication)")
    @PostMapping("/uploads")
    public ResponseEntity<VideoUploadResponse> createUpload(
            @Valid @RequestBody VideoUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoService.createVideoUpload(request));
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#videoId, authentication)")
    @PostMapping("/{videoId}/complete")
    public ResponseEntity<Void> completeUpload(
            @PathVariable Long videoId) {
        videoService.completeVideoUpload(videoId);
        // Accepted means conversion has been queued, not that playback is ready.
        return ResponseEntity.accepted().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/{videoId}/status")
    public java.util.Map<String, String> getStatus(@PathVariable Long videoId) {
        // Return a small status object instead of serializing JPA relationships.
        return java.util.Map.of("processingStatus", videoService.getProcessingStatus(videoId).name());
    }

    @PreAuthorize("@authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/{videoId}/playback")
    public ResponseEntity<VideoPlaybackResponse> getPlayback(
            @PathVariable Long videoId,
            @RequestParam(required = false) String quality) {
        return ResponseEntity.ok(videoService.getPlayback(videoId, quality));
    }

    @PreAuthorize("@authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/{videoId}/thumbnail")
    public ResponseEntity<VideoThumbnailResponse> getThumbnail(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(
                new VideoThumbnailResponse(videoService.getThumbnailUrl(videoId))
        );
    }
}
