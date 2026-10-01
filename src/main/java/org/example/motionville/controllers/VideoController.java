package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.VideoCreateRequest;
import org.example.motionville.dto.VideoPageResponse;
import org.example.motionville.dto.VideoProcessingStatusRequest;
import org.example.motionville.dto.VideoResponse;
import org.example.motionville.dto.VideoUpdateRequest;
import org.example.motionville.dto.VideoVisibilityRequest;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.services.videoService.VideoManagementService;
import org.example.motionville.services.videoService.VideoSearchService;
import org.example.motionville.services.videoService.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class VideoController {

    private final VideoManagementService managementService;
    private final VideoSearchService searchService;
    private final VideoService videoService;

    public VideoController(
            VideoManagementService managementService,
            VideoSearchService searchService,
            VideoService videoService) {
        this.managementService = managementService;
        this.searchService = searchService;
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<VideoResponse> createVideo(
            @Valid @RequestBody VideoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.create(request));
    }

    @GetMapping
    public VideoPageResponse getAllVideos(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(defaultValue = "false") boolean publicOnly) {
        return searchService.search(search, page, size, sort, categoryId, channelId, publicOnly);
    }

    @GetMapping("/{id}")
    public VideoResponse getVideo(@PathVariable Long id) {
        return managementService.getVideo(id);
    }

    @PutMapping("/{id}")
    public VideoResponse updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request) {
        return managementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        managementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public VideoResponse publishVideo(@PathVariable Long id) {
        return managementService.publish(id);
    }

    @PatchMapping("/{id}/unpublish")
    public VideoResponse unpublishVideo(@PathVariable Long id) {
        return managementService.unpublish(id);
    }

    @PatchMapping("/{id}/visibility")
    public VideoResponse changeVisibility(
            @PathVariable Long id,
            @Valid @RequestBody VideoVisibilityRequest request) {
        return managementService.changeVisibility(id, request);
    }

    @PatchMapping("/{id}/processing-status")
    public VideoResponse changeProcessingStatus(
            @PathVariable Long id,
            @Valid @RequestBody VideoProcessingStatusRequest request) {
        return managementService.changeProcessingStatus(id, request);
    }

    @GetMapping("/play/{id}")
    public List<VideoAsset> playVideo(@PathVariable Long id) {
        return videoService.playVideo(id);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> legacyDeleteVideo(@PathVariable Long id) {
        managementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
