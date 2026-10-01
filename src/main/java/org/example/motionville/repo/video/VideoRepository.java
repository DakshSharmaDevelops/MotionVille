package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.videoId = :id")
    Optional<Video> findForProcessing(@Param("id") Long id);

    java.util.List<Video> findAllByOrderByCreatedAtDesc();
}
