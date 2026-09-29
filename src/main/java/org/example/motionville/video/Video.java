package org.example.motionville.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.example.motionville.channel.Channel;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false, unique = true, length = 100)
    private String storageKey;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoVisibility visibility;

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Video() {
    }

    public Video(
            Channel channel,
            String title,
            String description,
            String category,
            String tags,
            String storageKey,
            String contentType,
            VideoVisibility visibility) {
        this.channel = channel;
        this.title = title;
        this.description = description;
        this.category = category;
        this.tags = tags;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.visibility = visibility;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getTags() {
        return tags;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public VideoVisibility getVisibility() {
        return visibility;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getViewCount() {
        return viewCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void update(String title, String description, String category, String tags, VideoVisibility visibility) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.tags = tags;
        this.visibility = visibility;
    }
}
