package org.example.motionville.entity.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.playlist.PlayList;
import jakarta.persistence.*;
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

    @Column(nullable = false,unique = true,length = 150)
    private String email;

    @Column(nullable = false, unique=true, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name="display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name="avatar_url")
    private String avatarUrl;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @OneToMany(mappedBy="owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Channel> channels;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayList> playLists;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
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
        if (role == null || role.isBlank()) role = "USER";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
