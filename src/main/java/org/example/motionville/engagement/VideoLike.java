package org.example.motionville.engagement;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.example.motionville.account.UserAccount;
import org.example.motionville.video.Video;

@Entity
@Table(name = "video_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"}))
public class VideoLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    private Instant createdAt = Instant.now();

    protected VideoLike() {
    }

    public VideoLike(UserAccount user, Video video) {
        this.user = user;
        this.video = video;
    }
}
