package org.example.motionville.entity.account;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.playlist.PlayList;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique=true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String password;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name="display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name="avatar_url")
    private String avatarUrl;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy="owner")
    private List<Channel> channels;

    @OneToMany(mappedBy = "owner")
    private List<PlayList> playLists;

    @OneToMany(mappedBy = "author")
    private List<Comment> comments;
}
