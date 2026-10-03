package org.example.motionville.services.report;

import org.example.motionville.dto.report.ReportStatusUpdateRequest;
import org.example.motionville.dto.report.ReportCreateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportsServiceImplementsTest {

    private ReportRepository reportRepository;
    private AppUserRepository appUserRepository;
    private VideoRepository videoRepository;
    private ReportsServiceImplements reportsService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        videoRepository = mock(VideoRepository.class);
        reportsService = new ReportsServiceImplements(
                reportRepository,
                appUserRepository,
                videoRepository,
                mock(CommentRepository.class));
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
        verify(reportRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
