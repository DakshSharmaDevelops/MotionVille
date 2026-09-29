package org.example.motionville.entity.comment;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.enums.engagement.ReactionType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id",nullable = false)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    private ReactionType reaction;

    @Column(name="created_at",nullable = false)
    private Instant createdAt;
}
