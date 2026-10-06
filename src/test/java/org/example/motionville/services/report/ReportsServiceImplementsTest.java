package org.example.motionville.services.report;

import org.example.motionville.dto.report.ReportStatusUpdateRequest;
import org.example.motionville.dto.report.ReportCreateRequest;
import org.example.motionville.dto.report.ReportResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.report.Report;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.account.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportsServiceImplementsTest {

    private ReportRepository reportRepository;
    private AppUserRepository appUserRepository;
    private VideoRepository videoRepository;
    private EmailService emailService;
    private ReportsServiceImplements reportsService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        videoRepository = mock(VideoRepository.class);
        emailService = mock(EmailService.class);
        reportsService = new ReportsServiceImplements(
                reportRepository,
                appUserRepository,
                videoRepository,
                mock(CommentRepository.class),
                emailService);
        ReflectionTestUtils.setField(reportsService, "demoAdminUserId", 9L);
    }

    @Test
    void rejectsNonAdminForEveryReportManagementOperation() {
        ResponseStatusException listError = assertThrows(
                ResponseStatusException.class,
                () -> reportsService.getAllReports(8L));
        ResponseStatusException detailError = assertThrows(
                ResponseStatusException.class,
                () -> reportsService.getReportById(1L, 8L));
        ResponseStatusException updateError = assertThrows(
                ResponseStatusException.class,
                () -> reportsService.updateReportStatus(
                        1L, 8L, new ReportStatusUpdateRequest(ReportStatus.RESOLVED)));

        assertEquals(403, listError.getStatusCode().value());
        assertEquals(403, detailError.getStatusCode().value());
        assertEquals(403, updateError.getStatusCode().value());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void rejectsAnotherActiveReportFromSameReporterForSameVideo() {
        AppUser reporter = new AppUser();
        reporter.setId(3L);
        Video video = new Video();
        video.setVideoId(12L);
        when(appUserRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(reporter));
        when(videoRepository.findById(12L)).thenReturn(Optional.of(video));
        when(reportRepository.existsByReporter_IdAndVideo_VideoIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(12L),
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(true);

        ReportCreateRequest request = new ReportCreateRequest();
        request.setReporterId(3L);
        request.setVideoId(12L);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> reportsService.saveReport(request));

        assertEquals(409, error.getStatusCode().value());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void saveReportSendsEmailToConfiguredAdminAndDatabaseAdmins() {
        AppUser reporter = new AppUser();
        reporter.setId(3L);
        reporter.setUsername("reporterUser");
        reporter.setEmail("reporter@example.com");

        Video video = new Video();
        video.setVideoId(12L);
        video.setTitle("Suspicious Video");

        when(appUserRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(reporter));
        when(videoRepository.findById(12L)).thenReturn(Optional.of(video));
        when(reportRepository.existsByReporter_IdAndVideo_VideoIdAndStatusIn(
                eq(3L), eq(12L), any())).thenReturn(false);

        AppUser dbAdmin = new AppUser();
        dbAdmin.setId(10L);
        dbAdmin.setUsername("superadmin");
        dbAdmin.setDisplayName("Super Admin");
        dbAdmin.setEmail("admin@motionville.com");
        dbAdmin.setRole("ADMIN");
        when(appUserRepository.findAllAdmins()).thenReturn(List.of(dbAdmin));

        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(42L);
            return r;
        });

        ReflectionTestUtils.setField(reportsService, "configuredAdminEmail", "owner@company.com");

        ReportCreateRequest request = new ReportCreateRequest();
        request.setReporterId(3L);
        request.setVideoId(12L);
        request.setReason(ReportReason.SPAM);
        request.setDetails("Commercial spam link in title");

        ReportResponse response = reportsService.saveReport(request);

        assertNotNull(response);
        assertEquals(42L, response.id());
        assertEquals(3L, response.reporterId());

        // Verify email was sent to configured admin
        verify(emailService).sendReportNotificationEmail(eq("owner@company.com"), eq("Administrator"), any(Report.class));
        // Verify email was sent to database admin
        verify(emailService).sendReportNotificationEmail(eq("admin@motionville.com"), eq("Super Admin"), any(Report.class));
    }

    @Test
    void saveReportSucceedsEvenIfEmailSendingFails() {
        AppUser reporter = new AppUser();
        reporter.setId(3L);
        Video video = new Video();
        video.setVideoId(12L);

        when(appUserRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(reporter));
        when(videoRepository.findById(12L)).thenReturn(Optional.of(video));
        when(reportRepository.existsByReporter_IdAndVideo_VideoIdAndStatusIn(
                eq(3L), eq(12L), any())).thenReturn(false);

        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(43L);
            return r;
        });

        ReflectionTestUtils.setField(reportsService, "configuredAdminEmail", "admin@domain.com");
        doThrow(new RuntimeException("SMTP connection timed out"))
                .when(emailService).sendReportNotificationEmail(any(), any(), any());

        ReportCreateRequest request = new ReportCreateRequest();
        request.setReporterId(3L);
        request.setVideoId(12L);
        request.setReason(ReportReason.HARASSMENT);

        ReportResponse response = reportsService.saveReport(request);

        assertNotNull(response);
        assertEquals(43L, response.id());
    }
}
