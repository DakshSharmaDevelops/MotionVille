package org.example.motionville.entity.playlist;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.enums.playlist.PlayListVisibility;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name="playlists")
public class PlayList {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="owner_id",nullable = false)
    private AppUser owner;

    @Column(nullable = false,length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private PlayListVisibility visibility=PlayListVisibility.PRIVATE;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="updated_at",nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "playList",
                cascade = CascadeType.ALL,
                orphanRemoval = true)
    @Builder.Default
    private List<PlayListVideo> videos;

}
