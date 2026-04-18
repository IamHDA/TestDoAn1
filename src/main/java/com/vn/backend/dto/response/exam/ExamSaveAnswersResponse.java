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
public class ExamSaveAnswersResponse {
    private Long sessionExamId;
    private Long studentId;
    private LocalDateTime savedAt;
    private String message;
}

