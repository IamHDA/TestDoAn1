package com.vn.backend.dto.request.answer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerUpdateRequest {
    private String content;
    private Boolean isCorrect;
    private Integer displayOrder;
}
