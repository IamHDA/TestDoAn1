package com.vn.backend.dto.request.exam;

import lombok.Data;
import java.util.List;

@Data
public class ExamSubmissionRequest {
    private Long sessionExamId;
    private List<AnswerSubmission> answers;

    @Data
    public static class AnswerSubmission {
        private Long questionSnapshotId; // ID của ExamQuestionSnapshot
        private List<Long> selectedAnswerIds; // List answer IDs (cho multiple choice)
    }
}

