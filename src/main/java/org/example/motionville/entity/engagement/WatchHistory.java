package org.example.motionville.entity.engagement;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
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
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uq_watch_history_user_video",
                        columnNames = {"user_id","video_id"}
                )
        }
)
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false,name="last_position_seconds")
    private Integer lastPositionSeconds=0;

    @Column(name="last_watched_at", nullable = false)
    private Instant lastWatchedAt;
}
