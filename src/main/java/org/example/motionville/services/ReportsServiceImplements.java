package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.ReportCreateRequest;
import org.example.motionville.dto.ReportResponse;
import org.example.motionville.dto.ReportStatusUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.report.Report;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsServiceImplements implements ReportsService {

    private final ReportRepository reportRepository;
    private final AppUserRepository appUserRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId) {
        return toResponse(findReport(reportId));
    }

    @Override
    @Transactional
    public ReportResponse saveReport(ReportCreateRequest request) {
        AppUser reporter = appUserRepository.findById(request.getReporterId())
                .orElseThrow(() -> notFound("Reporter"));
        Video video = request.getVideoId() == null ? null : videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> notFound("Video"));
        Comment comment = request.getCommentId() == null ? null
                : commentRepository.findById(request.getCommentId())
                .orElseThrow(() -> notFound("Comment"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setVideo(video);
        report.setComment(comment);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setStatus(ReportStatus.OPEN);
        return toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public ReportResponse updateReportStatus(Long reportId, ReportStatusUpdateRequest request) {
        Report report = findReport(reportId);
        ReportStatus status = request.status();
        report.setStatus(status);
        report.setResolvedAt(status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED
                ? Instant.now() : null);
        return toResponse(reportRepository.save(report));
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> notFound("Report"));
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getVideo() == null ? null : report.getVideo().getVideoId(),
                report.getComment() == null ? null : report.getComment().getId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}
