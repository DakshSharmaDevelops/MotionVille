package org.example.motionville.controllers;

import org.example.motionville.dto.ReportCreateRequest;
import org.example.motionville.dto.ReportResponse;
import org.example.motionville.dto.ReportStatusUpdateRequest;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.example.motionville.services.ReportsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ReportsController(new StubReportsService())).build();
    }

    @Test
    void createsReportUsingRequestAndResponseDtos() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reporterId":3,"videoId":12,"reason":"SPAM","details":"Spam content"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reporterId").value(3))
                .andExpect(jsonPath("$.videoId").value(12))
                .andExpect(jsonPath("$.commentId").doesNotExist())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsRequestWithoutExactlyOneTarget() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reporterId":3,"reason":"SPAM"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsRequestWithBothTargets() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reporterId":3,"videoId":12,"commentId":8,"reason":"SPAM"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsReportsAsResponseDtos() throws Exception {
        mockMvc.perform(get("/api/reports").param("userId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reason").value("SPAM"));
    }

    @Test
    void updatesOnlyReportStatus() throws Exception {
        mockMvc.perform(put("/api/reports/1/status")
                        .param("userId", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundForMissingReport() throws Exception {
        mockMvc.perform(get("/api/reports/99").param("userId", "9"))
                .andExpect(status().isNotFound());
    }

    private static class StubReportsService implements ReportsService {
        @Override
        public List<ReportResponse> getAllReports(Long requesterId) {
            return List.of(response(ReportStatus.OPEN));
        }

        @Override
        public ReportResponse getReportById(Long reportId, Long requesterId) {
            if (reportId != 1L) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
            }
            return response(ReportStatus.OPEN);
        }

        @Override
        public ReportResponse saveReport(ReportCreateRequest request) {
            return response(ReportStatus.OPEN);
        }

        @Override
        public ReportResponse updateReportStatus(
                Long reportId,
                Long requesterId,
                ReportStatusUpdateRequest request) {
            if (reportId != 1L) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
            }
            return response(request.status());
        }

        private ReportResponse response(ReportStatus status) {
            return new ReportResponse(
                    1L, 3L, 12L, null, ReportReason.SPAM, "Spam content", status,
                    Instant.parse("2026-01-01T00:00:00Z"),
                    status == ReportStatus.RESOLVED ? Instant.parse("2026-01-01T00:00:00Z") : null
            );
        }
    }
}
