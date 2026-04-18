package com.vn.backend.dto.response.session;

import com.vn.backend.dto.response.answer.AnswerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextQuestionResponse {
    private Long questionId;
    private String questionText;
    private String imageUrl;
    private Integer difficultyLevel;
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    private List<AnswerResponse> answers; // KHÔNG chứa isCorrect
}

