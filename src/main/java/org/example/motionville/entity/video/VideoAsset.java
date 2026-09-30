package org.example.motionville.entity.video;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(
        name = "video_assets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_video_asset_variant",
                        columnNames = {"video_id","quality","mime_type"}
                )
        }
)
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id",nullable = false)
    private Video video;

    @Column(name = "asset_url",nullable = false,columnDefinition = "TEXT")
    private String assetUrl;

    @Column(nullable = false,length = 20)
    private String quality;

    @Column(name = "mime_type",length = 100,nullable = false)
    private String mimeType;

    @Column(name="size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    protected VideoAsset() {
    }

}
