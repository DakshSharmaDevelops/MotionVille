package org.example.motionville.repo.engagement;

import org.example.motionville.entity.engagement.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Optional<WatchHistory> findByUser_IdAndVideo_VideoId(Long userId, Long videoId);

    List<WatchHistory> findByUser_IdOrderByLastWatchedAtDesc(Long userId);

    long deleteAllByUser_Id(Long userId);

    void deleteAllByVideo_VideoId(Long videoId);
}
