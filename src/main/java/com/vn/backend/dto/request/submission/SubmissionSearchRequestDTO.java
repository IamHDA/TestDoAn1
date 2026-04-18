package com.vn.backend.dto.request.submission;

import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionSearchRequestDTO {
    private String username;
    private String fullName;
    private SubmissionStatus submissionStatus;
    private GradingStatus gradingStatus;
    private Long assignmentId;
}
