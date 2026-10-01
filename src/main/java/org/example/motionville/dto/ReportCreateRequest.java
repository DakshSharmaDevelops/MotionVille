package org.example.motionville.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.motionville.entity.report.enums.ReportReason;

@Getter
@Setter
@NoArgsConstructor
public class ReportCreateRequest {

    @NotNull
    private Long reporterId;

    private Long videoId;

    private Long commentId;

    @NotNull
    private ReportReason reason;

    @Size(max = 10000)
    private String details;

    @AssertTrue(message = "Exactly one of videoId or commentId must be provided")
    public boolean isTargetValid() {
        return (videoId == null) != (commentId == null);
    }
}
