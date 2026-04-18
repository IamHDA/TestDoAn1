package com.vn.backend.dto.response.exam;

import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.ExamSubmissionStatus;
import com.vn.backend.enums.QuestionOrderMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentExamOverviewResponse {
    // Session exam information
    private Long sessionExamId;
    private Long examId;
    private String title;
    private String description;
    private Long duration; // minutes
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ExamMode examMode;
    private Boolean isProctored;
    private QuestionOrderMode questionOrderMode;
    private Boolean isInstantlyResult;
    private Integer totalQuestions; // Số lượng câu hỏi trong ca thi này
    
    // Student status information
    private Long studentSessionExamId;
    private ExamSubmissionStatus submissionStatus;
    private LocalDateTime examStartTime;
    private LocalDateTime submissionTime;
    private Double score;
    private Boolean canStartExam;
    private Boolean canContinueExam;
}

