package com.vn.backend.dto.request.session;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;
    
    @NotNull(message = "Selected answer ID is required")
    private Long selectedAnswerId;
    
    private Long responseTimeSeconds; // Optional: thời gian phản hồi
}

