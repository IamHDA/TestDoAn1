package com.vn.backend.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSubmissionResponse {
    private Long studentSessionExamId;
    private Long sessionExamId;
    private Double score;
    private LocalDateTime submissionTime;
    private String message;
}

