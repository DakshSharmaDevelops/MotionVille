package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
