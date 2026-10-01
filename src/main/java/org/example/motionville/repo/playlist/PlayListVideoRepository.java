package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayListVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayListVideoRepository extends JpaRepository<PlayListVideo, Long> {
    List<PlayListVideo> findByPlayList_IdOrderByPositionAsc(Long playListId);

    Optional<PlayListVideo> findByPlayList_IdAndVideo_VideoId(Long playListId, Long videoId);
}
