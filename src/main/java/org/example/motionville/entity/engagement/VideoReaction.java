package org.example.motionville.entity.engagement;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.engagement.enums.ReactionType;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
@Table(
        name="video_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uq_video_reaction_user_video",
                        columnNames = {"user_id","video_id"}
                )
        }
)
public class VideoReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private ReactionType reaction;

    @Column(nullable = false,name="created_at")
    private Instant createdAt;
}
