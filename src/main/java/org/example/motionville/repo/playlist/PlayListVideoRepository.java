package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayListVideo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayListVideoRepository extends JpaRepository<PlayListVideo, Long> {
    @EntityGraph(attributePaths = {"video"})
    List<PlayListVideo> findByPlayList_IdOrderByPositionAsc(Long playListId);

    @EntityGraph(attributePaths = {"video", "playList"})
    Optional<PlayListVideo> findByPlayList_IdAndVideo_VideoId(Long playListId, Long videoId);
}
