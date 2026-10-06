package org.example.motionville.entity.engagement;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "video_views",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_video_view_video_session",
                columnNames = {"video_id", "session_id"}
        )
)
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id",nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser viewer;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;
}
