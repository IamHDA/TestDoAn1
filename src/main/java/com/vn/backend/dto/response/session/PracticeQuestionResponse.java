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
public class PracticeQuestionResponse {
    private Long questionId;
    private String questionText;
    private String imageUrl;
    private String questionType; // SINGLE_CHOICE, MULTI_CHOICE
    private Integer difficultyLevel;
    private List<AnswerResponse> answers; // Bao gồm isCorrect và correctAnswerId
    private Long correctAnswerId; // ID của đáp án đúng (để frontend tự check)
}

