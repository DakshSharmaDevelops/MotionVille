package org.example.motionville.services.videoService;

import org.example.motionville.dto.VideoAssetRequest;
import org.example.motionville.dto.VideoAssetResponse;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.repo.video.VideoAssetRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VideoAssetManagementService {

    private final VideoRepository videoRepository;
    private final VideoAssetRepository assetRepository;
    private final R2StorageService r2StorageService;

    public VideoAssetManagementService(
            VideoRepository videoRepository,
            VideoAssetRepository assetRepository,
            @Lazy R2StorageService r2StorageService) {
        this.videoRepository = videoRepository;
        this.assetRepository = assetRepository;
        this.r2StorageService = r2StorageService;
    }

    @Transactional
    public VideoAssetResponse add(Long videoId, VideoAssetRequest request) {
        Video video = findVideo(videoId);
        String quality = request.quality().trim();
        String mimeType = request.mimeType().trim();
        if (assetRepository.existsByVideo_VideoIdAndQualityAndMimeType(videoId, quality, mimeType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Video asset variant already exists");
        }

        VideoAsset asset = VideoAsset.builder()
                .video(video)
                .assetUrl(request.assetUrl().trim())
                .quality(quality)
                .mimeType(mimeType)
                .sizeBytes(request.sizeBytes())
                .createdAt(Instant.now())
                .build();
        return toResponse(assetRepository.save(asset));
    }

    public List<VideoAssetResponse> getAll(Long videoId) {
        Video video = findVideo(videoId);
        return assetRepository.findByVideo(video).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long videoId, Long assetId) {
        VideoAsset asset = assetRepository.findByIdAndVideo_VideoId(assetId, videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video asset not found"));
        r2StorageService.deleteAfterCommit(List.of(asset.getAssetUrl()));
        assetRepository.delete(asset);
    }

    private Video findVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    }

    private VideoAssetResponse toResponse(VideoAsset asset) {
        return new VideoAssetResponse(
                asset.getId(),
                asset.getVideo().getVideoId(),
                asset.getAssetUrl(),
                asset.getQuality(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getCreatedAt()
        );
    }
}
