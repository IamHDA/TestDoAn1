package com.vn.backend.dto.request.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionBulkCreateRequest {

    @NotEmpty(message = "Questions payload is required")
    @Valid
    private List<QuestionBulkCreateItemRequest> questions;
}

