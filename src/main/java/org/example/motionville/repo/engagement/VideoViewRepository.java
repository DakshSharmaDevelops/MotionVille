package org.example.motionville.repo.engagement;

import org.example.motionville.entity.engagement.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
    Optional<VideoView> findByVideo_VideoIdAndSessionId(Long videoId, String sessionId);

    long countByVideo_VideoId(Long videoId);

    long countByVideo_Channel_ChannelId(Long channelId);

    void deleteAllByVideo_VideoId(Long videoId);
}
