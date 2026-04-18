package com.vn.backend.dto.response.exam;

import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.enums.ExamSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestionsResponse {
    private Long sessionExamId;
    private Long examId;
    private String title;
    private Long duration;
    private LocalDateTime examStartTime;
    private LocalDateTime endDate;
    private Boolean canSubmit;
    private ExamSubmissionStatus submissionStatus;
    private List<ExamQuestionSnapshotResponse> questions;
}

