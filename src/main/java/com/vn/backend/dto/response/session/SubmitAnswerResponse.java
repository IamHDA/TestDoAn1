package com.vn.backend.dto.response.session;

import com.vn.backend.dto.response.answer.AnswerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {
    private Boolean wasCorrect;
    private Long correctAnswerId; // Giữ lại để backward compatibility
    private AnswerResponse correctAnswer; // Thông tin đầy đủ của câu trả lời đúng
    private Double newMasteryScore;
    private String message; // Optional: feedback message
    private Long questionId;
    private Long topicId;
    private String topicName;
}

