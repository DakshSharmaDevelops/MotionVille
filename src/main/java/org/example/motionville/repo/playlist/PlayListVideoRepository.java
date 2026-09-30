package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayListVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayListVideoRepository extends JpaRepository<PlayListVideo, Long> {
}
