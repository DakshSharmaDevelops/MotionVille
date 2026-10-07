package org.example.motionville.entity.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.entity.playlist.PlayList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.report.Report;

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

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailVerificationToken> verificationTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> passwordResetTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshToken> refreshTokens;

    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Channel channel;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayList> playLists;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> receivedNotifications;

    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports;

    public void setChannel(Channel channel) {
        if (channel == null) {
            if (this.channel != null) {
                this.channel.setOwner(null);
            }
        } else {
            channel.setOwner(this);
        }
        this.channel = channel;
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
