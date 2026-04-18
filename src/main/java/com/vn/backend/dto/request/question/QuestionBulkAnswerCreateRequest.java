package com.vn.backend.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionBulkAnswerCreateRequest {

    @NotBlank(message = "Answer content is required")
    private String content;

    @NotNull(message = "IsCorrect flag is required")
    private Boolean isCorrect;

    private Integer displayOrder;
}

