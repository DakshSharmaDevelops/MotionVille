package org.example.motionville.services.report;

import org.example.motionville.dto.report.ReportCreateRequest;
import org.example.motionville.dto.report.ReportResponse;
import org.example.motionville.dto.report.ReportStatusUpdateRequest;

import java.util.List;

public interface ReportsService {
    List<ReportResponse> getAllReports(Long requesterId);

    ReportResponse getReportById(Long reportId, Long requesterId);

    ReportResponse saveReport(ReportCreateRequest request);

    ReportResponse updateReportStatus(Long reportId, Long requesterId, ReportStatusUpdateRequest request);
}
