package com.vn.backend.dto.response.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSetResponse {
    private Long topicId;
    private String topicName;
    private Integer totalQuestions;
    private String subjectName;
    private List<PracticeQuestionResponse> questions; // Tất cả câu hỏi đã được shuffle
}

