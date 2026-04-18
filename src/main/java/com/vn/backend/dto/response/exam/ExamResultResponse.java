package com.vn.backend.dto.response.exam;

import com.vn.backend.enums.ExamSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResultResponse {
    private Long studentSessionExamId;
    private Long sessionExamId;
    private Long studentId;
    private String studentFullName;
    private String studentUsername;
    private String studentCode;
    private String studentEmail;
    private String studentAvatarUrl;
    private Double score;
    private ExamSubmissionStatus submissionStatus;
    private LocalDateTime examStartTime;
    private LocalDateTime submissionTime;
    private LocalDateTime createdAt;
}

