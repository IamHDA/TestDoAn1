package com.vn.backend.dto.request.question;

import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.enums.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionCreateRequest {
    @NotNull(message = "Content is required")
    private String content;
    private String imageUrl;
    @NotNull(message = "Type is required")
    private QuestionType type;
    @NotNull(message = "Topic ID is required")
    private Long topicId;
    private Integer difficultyLevel;

    private List<AnswerCreateRequest> answers;
}
