package com.vn.backend.dto.response.submission;

import com.vn.backend.utils.DateUtils;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionExcelExportDTO {
    private int index;                // STT
    private String username;
    private String fullName;
    private String code;
    private String submissionStatus;
    private String gradingStatus;
    private String submittedAt;       // Format dd/MM/yyyy HH:mm
    private Double grade;

    public static SubmissionExcelExportDTO toExcelDTO(SubmissionExcelQueryDTO p, int index) {
        String submissionStatusText = switch (p.getSubmissionStatus()) {
            case NOT_SUBMITTED -> "Chưa nộp";
            case SUBMITTED -> "Đã nộp";
            case LATE_SUBMITTED -> "Nộp muộn";
        };

        String gradingStatusText = switch (p.getGradingStatus()) {
            case GRADED -> "Đã chấm";
            case NOT_GRADED -> "Chưa chấm";
        };

        String submittedAtText = "";
        if (p.getSubmittedAt() != null) {
            submittedAtText = DateUtils.format(p.getSubmittedAt(), DateUtils.YYYY_MM_DD_HH_MM);
        }

        return SubmissionExcelExportDTO.builder()
                .index(index)
                .username(p.getUsername())
                .fullName(p.getFullName())
                .code(p.getCode())
                .submissionStatus(submissionStatusText)
                .gradingStatus(gradingStatusText)
                .submittedAt(submittedAtText)
                .grade(p.getGrade())
                .build();
    }
}
