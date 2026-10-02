package org.example.motionville.services;

import org.example.motionville.dto.ReportStatusUpdateRequest;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.report.ReportRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ReportsServiceImplementsTest {

    private ReportRepository reportRepository;
    private ReportsServiceImplements reportsService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        reportsService = new ReportsServiceImplements(
                reportRepository,
                mock(AppUserRepository.class),
                mock(VideoRepository.class),
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
}
