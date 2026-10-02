package org.example.motionville.services;

import org.example.motionville.dto.ReportCreateRequest;
import org.example.motionville.dto.ReportResponse;
import org.example.motionville.dto.ReportStatusUpdateRequest;

import java.util.List;

public interface ReportsService {
    List<ReportResponse> getAllReports(Long requesterId);

    ReportResponse getReportById(Long reportId, Long requesterId);

    ReportResponse saveReport(ReportCreateRequest request);

    ReportResponse updateReportStatus(Long reportId, Long requesterId, ReportStatusUpdateRequest request);
}
