package org.example.motionville.dto;

import jakarta.validation.constraints.NotNull;
import org.example.motionville.entity.report.enums.ReportStatus;

public record ReportStatusUpdateRequest(@NotNull ReportStatus status) {
}
