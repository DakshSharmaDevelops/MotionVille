package org.example.motionville.dto;

import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;

import java.time.Instant;

public record ReportResponse(
        Long id,
        Long reporterId,
        Long videoId,
        Long commentId,
        ReportReason reason,
        String details,
        ReportStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}
