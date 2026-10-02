package org.example.motionville.repo.report;

import org.example.motionville.entity.report.Report;
import org.example.motionville.entity.report.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporter_IdAndVideo_VideoIdAndStatusIn(
            Long reporterId, Long videoId, Collection<ReportStatus> statuses);

    boolean existsByReporter_IdAndComment_IdAndStatusIn(
            Long reporterId, Long commentId, Collection<ReportStatus> statuses);
}
