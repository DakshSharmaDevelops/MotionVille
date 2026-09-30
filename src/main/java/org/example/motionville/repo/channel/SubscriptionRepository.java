package org.example.motionville.repo.channel;

import org.example.motionville.entity.channel.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}
