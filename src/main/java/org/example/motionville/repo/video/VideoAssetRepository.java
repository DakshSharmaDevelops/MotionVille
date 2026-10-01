package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoAssetRepository
        extends JpaRepository<VideoAsset, Long> {

    List<VideoAsset> findByVideo(Video video);

    boolean existsByVideo_VideoIdAndQualityAndMimeType(
            Long videoId, String quality, String mimeType);

    java.util.Optional<VideoAsset> findByIdAndVideo_VideoId(Long assetId, Long videoId);
}