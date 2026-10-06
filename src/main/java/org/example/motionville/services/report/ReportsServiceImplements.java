package org.example.motionville.services.report;

import org.example.motionville.dto.report.ReportCreateRequest;
import org.example.motionville.dto.report.ReportResponse;
import org.example.motionville.dto.report.ReportStatusUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.report.Report;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.account.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportsServiceImplements implements ReportsService {

    private static final Logger log = LoggerFactory.getLogger(ReportsServiceImplements.class);

    private static final EnumSet<ReportStatus> ACTIVE_REPORT_STATUSES =
            EnumSet.of(ReportStatus.OPEN, ReportStatus.REVIEWING);

    private final ReportRepository reportRepository;
    private final AppUserRepository appUserRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final EmailService emailService;

    @Value("${motionville.demo-admin-user-id:1}")
    private Long demoAdminUserId;

    @Value("${motionville.mail.admin-email:}")
    private String configuredAdminEmail;

    @Autowired
    public ReportsServiceImplements(
            ReportRepository reportRepository,
            AppUserRepository appUserRepository,
            VideoRepository videoRepository,
            CommentRepository commentRepository,
            @Autowired(required = false) EmailService emailService) {
        this.reportRepository = reportRepository;
        this.appUserRepository = appUserRepository;
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.emailService = emailService;
    }

    public ReportsServiceImplements(
            ReportRepository reportRepository,
            AppUserRepository appUserRepository,
            VideoRepository videoRepository,
            CommentRepository commentRepository) {
        this(reportRepository, appUserRepository, videoRepository, commentRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(Long requesterId) {
        requireDemoAdmin(requesterId);
        return reportRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId, Long requesterId) {
        requireDemoAdmin(requesterId);
        return toResponse(findReport(reportId));
    }

    @Override
    @Transactional
    public ReportResponse saveReport(ReportCreateRequest request) {
        AppUser reporter = appUserRepository.findByIdForUpdate(request.getReporterId())
                .orElseThrow(() -> notFound("Reporter"));
        Video video = request.getVideoId() == null ? null : videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> notFound("Video"));
        Comment comment = request.getCommentId() == null ? null
                : commentRepository.findById(request.getCommentId())
                .orElseThrow(() -> notFound("Comment"));
        if (comment != null && comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Comment was deleted");
        }

        boolean duplicateActiveReport = video != null
                ? reportRepository.existsByReporter_IdAndVideo_VideoIdAndStatusIn(
                        reporter.getId(), video.getVideoId(), ACTIVE_REPORT_STATUSES)
                : reportRepository.existsByReporter_IdAndComment_IdAndStatusIn(
                        reporter.getId(), comment.getId(), ACTIVE_REPORT_STATUSES);
        if (duplicateActiveReport) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have an open report for this content");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setVideo(video);
        report.setComment(comment);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setStatus(ReportStatus.OPEN);
        Report savedReport = reportRepository.save(report);

        sendAdminReportNotifications(savedReport);

        return toResponse(savedReport);
    }

    @Override
    @Transactional
    public ReportResponse updateReportStatus(
            Long reportId,
            Long requesterId,
            ReportStatusUpdateRequest request) {
        requireDemoAdmin(requesterId);
        Report report = findReport(reportId);
        ReportStatus status = request.status();
        report.setStatus(status);
        report.setResolvedAt(status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED
                ? Instant.now() : null);
        return toResponse(reportRepository.save(report));
    }

    private void sendAdminReportNotifications(Report report) {
        if (emailService == null) {
            return;
        }

        try {
            Map<String, String> recipients = new LinkedHashMap<>();

            // 1. Configured admin email from properties
            if (configuredAdminEmail != null && !configuredAdminEmail.isBlank()) {
                recipients.put(configuredAdminEmail.trim(), "Administrator");
            }

            // 2. All registered admin users in database
            List<AppUser> admins = appUserRepository.findAllAdmins();
            if (admins != null) {
                for (AppUser admin : admins) {
                    if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                        String name = admin.getDisplayName() != null && !admin.getDisplayName().isBlank()
                                ? admin.getDisplayName()
                                : admin.getUsername();
                        recipients.putIfAbsent(admin.getEmail().trim(), name);
                    }
                }
            }

            // 3. Fallback to demo admin user if no admin emails found yet
            if (recipients.isEmpty() && demoAdminUserId != null) {
                appUserRepository.findById(demoAdminUserId).ifPresent(user -> {
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        String name = user.getDisplayName() != null && !user.getDisplayName().isBlank()
                                ? user.getDisplayName()
                                : user.getUsername();
                        recipients.putIfAbsent(user.getEmail().trim(), name);
                    }
                });
            }

            for (Map.Entry<String, String> entry : recipients.entrySet()) {
                try {
                    emailService.sendReportNotificationEmail(entry.getKey(), entry.getValue(), report);
                } catch (Exception ex) {
                    log.error("Failed to send report notification to '{}': {}", entry.getKey(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process admin report notifications for report #{}: {}", report.getId(), ex.getMessage());
        }
    }

    private void requireDemoAdmin(Long requesterId) {
        if (requesterId != null) {
            AppUser user = appUserRepository.findById(requesterId).orElse(null);
            if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                return;
            }
        }
        if (!demoAdminUserId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
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
