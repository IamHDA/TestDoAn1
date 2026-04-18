package com.vn.backend.dto.response.submission;

import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionExcelQueryDTO {
    private Long submissionId;
    private String username;
    private String fullName;
    private String code;
    private SubmissionStatus submissionStatus;
    private GradingStatus gradingStatus;
    private LocalDateTime submittedAt;
    private Double grade;
}
