package com.vn.backend.dto.request.question;
import com.vn.backend.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionSearchRequest {
    private Long subjectId;
    private Long topicId;
    private Integer difficultyLevel;
    private QuestionType type;
    private String content;
}
