package org.example.motionville.entity.channel;

import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long channelId;

    @OneToOne
    @JoinColumn(name="owner_id", unique = true, nullable = false)
    private AppUser owner;

    @Column(nullable = false, unique = true, length = 50)
    private String handle;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name="banner_url")
    private String bannerUrl;

    @Column(name="profile_image_url")
    private String profileImageUrl;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions;

    public void addVideo(Video video) {
        if (videos == null) videos = new ArrayList<>();
        videos.add(video);
        video.setChannel(this);
    }

    public void removeVideo(Video video) {
        if (videos != null) videos.remove(video);
    }

    public void addSubscription(Subscription subscription) {
        if (subscriptions == null) subscriptions = new ArrayList<>();
        subscriptions.add(subscription);
        subscription.setChannel(this);
    }

    public void removeSubscription(Subscription subscription) {
        if (subscriptions != null) subscriptions.remove(subscription);
    }

    public String getAvatarUrl() {
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            return profileImageUrl;
        }
        return owner != null ? owner.getAvatarUrl() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
