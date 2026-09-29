package org.example.motionville.entity.video;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.enums.video.VideoProcessingStatus;
import org.example.motionville.enums.video.VideoVisibility;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @OneToMany(mappedBy = "video")
    private List<Comment> comments;

    @ManyToMany
    @JoinTable(
            name = "video_tags",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

}
