package com.vn.backend.dto.response.question;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionSearchResponse {
    private Long id;
    private String content;
    private String type;
    private Integer difficultyLevel;
    private Long topicId;
    private String topicName;
    private Boolean isReviewQuestion;
}
