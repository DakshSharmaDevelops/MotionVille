package org.example.motionville.engagement;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.example.motionville.account.UserAccount;
import org.example.motionville.channel.Channel;

@Entity
@Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"subscriber_id", "channel_id"}))
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private UserAccount subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    private Instant createdAt = Instant.now();

    protected Subscription() {
    }

    public Subscription(UserAccount subscriber, Channel channel) {
        this.subscriber = subscriber;
        this.channel = channel;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getSubscriber() {
        return subscriber;
    }

    public Channel getChannel() {
        return channel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
