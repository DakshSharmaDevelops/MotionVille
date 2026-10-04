package org.example.motionville.repo.channel;

import org.example.motionville.entity.channel.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriber_IdAndChannel_ChannelId(
            Long subscriberId,
            Long channelId
    );

    Optional<Subscription> findBySubscriber_IdAndChannel_ChannelId(
            Long subscriberId,
            Long channelId
    );

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    List<Subscription> findBySubscriber_Id(Long subscriberId);

    @EntityGraph(attributePaths = {"subscriber"})
    List<Subscription> findByChannel_ChannelId(Long channelId);

    long countByChannel_ChannelId(Long channelId);

    void deleteBySubscriber_IdAndChannel_ChannelId(
            Long subscriberId,
            Long channelId
    );
}
