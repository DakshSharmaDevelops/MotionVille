package org.example.motionville.entity.comment;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name="comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="video_id", nullable = false)
    private Video video;

    @ManyToOne
    @JoinColumn(name="author_id",nullable = false)
    private AppUser author;

    @ManyToOne
    @JoinColumn(name="parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean deleted = false;

    @OneToMany(mappedBy = "parentComment")
    private List<Comment> replies;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String body;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @Column(name="updated_at",nullable = false)
    private Instant updatedAt;

    public void addReply(Comment reply) {
        if (replies == null) replies = new ArrayList<>();
        replies.add(reply);
        reply.setParentComment(this);
        reply.setVideo(video);
    }

    public void removeReply(Comment reply) {
        if (replies != null && replies.remove(reply)) reply.setParentComment(null);
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
