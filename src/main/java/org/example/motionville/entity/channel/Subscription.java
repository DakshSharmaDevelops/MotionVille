package org.example.motionville.entity.channel;

import org.example.motionville.entity.account.AppUser;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_subscription_user_channel",
                        columnNames = {"subscriber_id","channel_id"}
                )
        }
)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id",nullable = false)
    private AppUser subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id",nullable = false)
    private Channel channel;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt;
}
