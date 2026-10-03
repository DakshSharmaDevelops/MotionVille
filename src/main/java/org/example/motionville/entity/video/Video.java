package org.example.motionville.entity.video;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.engagement.VideoReaction;
import org.example.motionville.entity.playlist.PlayListVideo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long videoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false,length=255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name="duration_seconds", nullable = false)
    private Integer durationSeconds=0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoVisibility visibility= VideoVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name="processing_status", nullable = false,length = 20)
    private VideoProcessingStatus processingStatus= VideoProcessingStatus.UPLOADING;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name="published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "video",
                cascade = CascadeType.ALL,
                orphanRemoval = true)
    private List<VideoAsset> assets;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoReaction> videoReactions;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayListVideo> playlistEntries;

    @ManyToMany
    @JoinTable(
            name = "video_tags",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uq_video_tags_video_tag",
                    columnNames = {"video_id", "tag_id"})
    )
    private List<Tag> tags;

    public void addAsset(VideoAsset asset) {
        if (assets == null) assets = new ArrayList<>();
        assets.add(asset);
        asset.setVideo(this);
    }

    public void removeAsset(VideoAsset asset) {
        if (assets != null && assets.remove(asset)) {
            asset.setVideo(null);
        }
    }

    public void addComment(Comment comment) {
        if (comments == null) comments = new ArrayList<>();
        comments.add(comment);
        comment.setVideo(this);
    }

    public void removeComment(Comment comment) {
        if (comments != null) comments.remove(comment);
    }

    public void addVideoReaction(VideoReaction videoReaction) {
        if (videoReactions == null) videoReactions = new ArrayList<>();
        videoReactions.add(videoReaction);
        videoReaction.setVideo(this);
    }

    public void removeVideoReaction(VideoReaction videoReaction) {
        if (videoReactions != null) videoReactions.remove(videoReaction);
    }

    public void addTag(Tag tag) {
        if (tags == null) tags = new ArrayList<>();
        tags.add(tag);
        if (tag.getVideos() == null) tag.setVideos(new ArrayList<>());
        tag.getVideos().add(this);
    }

    public void removeTag(Tag tag) {
        if (tags != null && tags.remove(tag) && tag.getVideos() != null) {
            tag.getVideos().remove(this);
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
