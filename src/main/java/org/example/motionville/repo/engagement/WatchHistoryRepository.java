package org.example.motionville.repo.engagement;

import org.example.motionville.entity.engagement.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
}
