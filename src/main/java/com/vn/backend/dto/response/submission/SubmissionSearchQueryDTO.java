package com.vn.backend.dto.response.submission;

import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionSearchQueryDTO {
    private Long submissionId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String fullName;

    private SubmissionStatus submissionStatus;
    private GradingStatus gradingStatus;
    private LocalDateTime submittedAt;
    private Double grade;
}
