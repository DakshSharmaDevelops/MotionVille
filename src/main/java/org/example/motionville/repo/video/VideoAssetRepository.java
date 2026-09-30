package org.example.motionville.repo.video;

import org.example.motionville.entity.video.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
}
