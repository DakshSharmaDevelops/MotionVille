package org.example.motionville.engagement;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    List<Subscription> findBySubscriberIdOrderByCreatedAtDesc(Long subscriberId);

    long countByChannelId(Long channelId);
}
