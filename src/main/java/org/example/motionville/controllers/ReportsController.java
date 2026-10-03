package org.example.motionville.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.ReportCreateRequest;
import org.example.motionville.dto.ReportResponse;
import org.example.motionville.dto.ReportStatusUpdateRequest;
import org.example.motionville.services.ReportsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<ReportResponse> getAllReports(@RequestParam Long userId) {
        return reportsService.getAllReports(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{reportId}")
    public ReportResponse getReport(
            @PathVariable Long reportId,
            @RequestParam Long userId) {
        return reportsService.getReportById(reportId, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#request.reporterId, authentication)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(@Valid @RequestBody ReportCreateRequest request) {

        return reportsService.saveReport(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reportId}/status")
    public ReportResponse updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam Long userId,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return reportsService.updateReportStatus(reportId, userId, request);
    }
}