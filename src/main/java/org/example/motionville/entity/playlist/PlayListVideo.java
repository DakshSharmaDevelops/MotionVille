package org.example.motionville.entity.playlist;

import org.example.motionville.entity.video.Video;
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
        name="playlist_videos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uq_playlist_video",
                        columnNames = {"play_list_id","video_id"}
                ),
                @UniqueConstraint(
                        name = "uq_playlist_position",
                        columnNames = {"play_list_id","position"}
                )
        }
)
public class PlayListVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="playList_id",nullable = false)
    private PlayList playList;

    @ManyToOne
    @JoinColumn(name="video_id",nullable = false)
    private Video video;

    @Column(nullable = false)
    private Integer position;

    @Column(name="added_at", nullable = false)
    private Instant addedAt;
}
