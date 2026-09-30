package org.example.motionville.entity.playlist;

import org.example.motionville.entity.account.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.playlist.enums.PlayListVisibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    private List<PlayListVideo> videos;

    public void addVideo(PlayListVideo playlistVideo) {
        if (videos == null) videos = new ArrayList<>();
        videos.add(playlistVideo);
        playlistVideo.setPlayList(this);
    }

    public void removeVideo(PlayListVideo playlistVideo) {
        if (videos != null && videos.remove(playlistVideo)) playlistVideo.setPlayList(null);
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
