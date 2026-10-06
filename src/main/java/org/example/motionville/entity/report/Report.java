package org.example.motionville.entity.report;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.video.Video;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;

import java.time.Instant;

@Entity
@Table(name= "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id",nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="video_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="comment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private ReportStatus status=ReportStatus.OPEN;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
