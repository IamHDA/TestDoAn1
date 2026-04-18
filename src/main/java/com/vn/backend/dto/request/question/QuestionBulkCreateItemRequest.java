package com.vn.backend.dto.request.question;

import com.vn.backend.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionBulkCreateItemRequest {
    @NotNull(message = "Question content is required")
    private String content;
    private String imageUrl;
    @NotNull(message = "Question type is required")
    private QuestionType type;
    @NotNull(message = "Topic ID is required")
    private Long topicId;
    private Integer difficultyLevel;

    @NotEmpty(message = "Answers are required")
    @Valid
    private List<QuestionBulkAnswerCreateRequest> answers;
}

