package org.example.motionville.entity.channel;

import lombok.*;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long channelId;

    @ManyToOne(optional = false)
    @JoinColumn(name="owner_id" , nullable = false)
    private AppUser owner;

    @Column(nullable = false, unique = true, length = 50)
    private String handle;    // read about it//

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name="banner_url")
    private String bannerUrl;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "channel")
    private List<Video> videos;

    @OneToMany(mappedBy = "channel")
    private List<Subscription> subscriptions;

}
