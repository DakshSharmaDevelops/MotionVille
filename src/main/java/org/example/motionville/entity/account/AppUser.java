package org.example.motionville.entity.account;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.playlist.PlayList;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
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

    public void addChannel(Channel channel) {
        if (channels == null) channels = new ArrayList<>();
        channels.add(channel);
        channel.setOwner(this);
    }

    public void removeChannel(Channel channel) {
        if (channels != null) channels.remove(channel);
    }

    public void addPlayList(PlayList playList) {
        if (playLists == null) playLists = new ArrayList<>();
        playLists.add(playList);
        playList.setOwner(this);
    }

    public void removePlayList(PlayList playList) {
        if (playLists != null) playLists.remove(playList);
    }

    public void addComment(Comment comment) {
        if (comments == null) comments = new ArrayList<>();
        comments.add(comment);
        comment.setAuthor(this);
    }

    public void removeComment(Comment comment) {
        if (comments != null) comments.remove(comment);
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
