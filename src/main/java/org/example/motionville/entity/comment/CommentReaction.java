package org.example.motionville.entity.comment;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.engagement.enums.ReactionType;
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
@Table(name = "comment_reactions",
            uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_comment_reaction_user_comment",
                        columnNames = {"user_id","comment_id"}
                )
            })
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "comment_id",nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private ReactionType reaction;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
