package com.vn.backend.dto.request.answer;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerCreateRequest {
    @NotNull(message = "Content role is required")
    private String content;
    @NotNull(message = "QuestionId role is required")
    private Long questionId;
    @NotNull(message = "IsCorrect role is required")
    private Boolean isCorrect;
    @NotNull(message = "DisplayOrder role is required")
    private Integer displayOrder;
}
