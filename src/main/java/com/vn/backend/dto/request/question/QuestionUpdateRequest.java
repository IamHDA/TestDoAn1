package com.vn.backend.dto.request.question;
import com.vn.backend.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionUpdateRequest {
    private String content;
    private String imageUrl;
    private QuestionType type;
    private Long topicId;
    private Integer difficultyLevel;
}
