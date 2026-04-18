package com.vn.backend.dto.request.exam;

import lombok.Data;
import java.util.List;

@Data
public class ExamSaveAnswersRequest {
    private Long sessionExamId;
    private List<AnswerData> answers;

    @Data
    public static class AnswerData {
        private Long questionSnapshotId;
        private List<Long> selectedAnswerIds;
    }
}

