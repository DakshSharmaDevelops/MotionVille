package org.example.motionville.controllers.video;

import jakarta.validation.Valid;
import org.example.motionville.dto.video.VideoCreateRequest;
import org.example.motionville.dto.video.VideoPageResponse;
import org.example.motionville.dto.video.VideoProcessingStatusRequest;
import org.example.motionville.dto.video.TagResponse;
import org.example.motionville.dto.video.VideoAssetRequest;
import org.example.motionville.dto.video.VideoAssetResponse;
import org.example.motionville.dto.video.VideoResponse;
import org.example.motionville.dto.video.VideoUpdateRequest;
import org.example.motionville.dto.video.VideoVisibilityRequest;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.services.video.VideoAssetManagementService;
import org.example.motionville.services.video.VideoManagementService;
import org.example.motionville.services.video.VideoSearchService;
import org.example.motionville.services.video.VideoService;
import org.example.motionville.services.video.VideoTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class VideoController {

    private final VideoManagementService managementService;
    private final VideoSearchService searchService;
    private final VideoService videoService;
    private final VideoTagService videoTagService;
    private final VideoAssetManagementService videoAssetManagementService;

    public VideoController(
            VideoManagementService managementService,
            VideoSearchService searchService,
            VideoService videoService,
            VideoTagService videoTagService,
            VideoAssetManagementService videoAssetManagementService) {
        this.managementService = managementService;
        this.searchService = searchService;
        this.videoService = videoService;
        this.videoTagService = videoTagService;
        this.videoAssetManagementService = videoAssetManagementService;
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isChannelOwner(#request.channelId(), authentication)")
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

    @GetMapping("/trending")
    public VideoPageResponse getTrendingVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchService.getTrending(page, size);
    }

    @PreAuthorize("@authorizationService.canViewVideo(#id, authentication)")
    @GetMapping("/{id}")
    public VideoResponse getVideo(@PathVariable Long id) {
        return managementService.getVideo(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (@authorizationService.isVideoOwner(#id, authentication) and @authorizationService.isChannelOwner(#request.channelId(), authentication))")
    @PutMapping("/{id}")
    public VideoResponse updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request) {
        return managementService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id) {
        managementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#id, authentication)")
    @PatchMapping("/{id}/publish")
    public VideoResponse publishVideo(@PathVariable Long id) {
        return managementService.publish(id);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#id, authentication)")
    @PatchMapping("/{id}/unpublish")
    public VideoResponse unpublishVideo(@PathVariable Long id) {
        return managementService.unpublish(id);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#id, authentication)")
    @PatchMapping("/{id}/visibility")
    public VideoResponse changeVisibility(
            @PathVariable Long id,
            @Valid @RequestBody VideoVisibilityRequest request) {
        return managementService.changeVisibility(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#id, authentication)")
    @PatchMapping("/{id}/processing-status")
    public VideoResponse changeProcessingStatus(
            @PathVariable Long id,
            @Valid @RequestBody VideoProcessingStatusRequest request) {
        return managementService.changeProcessingStatus(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#videoId, authentication)")
    @PostMapping("/{videoId}/tags/{tagId}")
    public ResponseEntity<Void> addTag(
            @PathVariable Long videoId,
            @PathVariable Long tagId) {
        videoTagService.addTag(videoId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#videoId, authentication)")
    @DeleteMapping("/{videoId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(
            @PathVariable Long videoId,
            @PathVariable Long tagId) {
        videoTagService.removeTag(videoId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/{videoId}/tags")
    public List<TagResponse> getTags(@PathVariable Long videoId) {
        return videoTagService.getTags(videoId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#videoId, authentication)")
    @PostMapping("/{videoId}/assets")
    public ResponseEntity<VideoAssetResponse> addAsset(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoAssetManagementService.add(videoId, request));
    }

    @PreAuthorize("@authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/{videoId}/assets")
    public List<VideoAssetResponse> getAssets(@PathVariable Long videoId) {
        return videoAssetManagementService.getAll(videoId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isVideoOwner(#videoId, authentication)")
    @DeleteMapping("/{videoId}/assets/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long videoId,
            @PathVariable Long assetId) {
        videoAssetManagementService.delete(videoId, assetId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.canViewVideo(#id, authentication)")
    @GetMapping("/play/{id}")
    public List<VideoAssetResponse> playVideo(@PathVariable Long id) {
        return videoService.playVideo(id).stream()
                .map(asset -> new VideoAssetResponse(
                        asset.getId(),
                        asset.getVideo().getVideoId(),
                        asset.getAssetUrl(),
                        asset.getQuality(),
                        asset.getMimeType(),
                        asset.getSizeBytes(),
                        asset.getCreatedAt()))
                .toList();
    }
}