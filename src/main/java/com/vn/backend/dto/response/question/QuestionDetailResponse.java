package com.vn.backend.dto.response.question;

import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.Topic;
import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class QuestionDetailResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private String type;
    private Integer difficultyLevel;
    private TopicResponse topic;
    private Long createdBy;
    private List<AnswerResponse> answers;
    private Boolean isReviewQuestion;

    /**
     * chưa map createdBy
     */
    public static QuestionDetailResponse fromEntity(Question entity) {
        QuestionDetailResponse questionDetailResponse = new QuestionDetailResponse();
        questionDetailResponse.setId(entity.getQuestionId());
        questionDetailResponse.setContent(entity.getContent());
        questionDetailResponse.setImageUrl(entity.getImageUrl());
        questionDetailResponse.setType(entity.getType().toString());
        questionDetailResponse.setDifficultyLevel(entity.getDifficultyLevel());
        questionDetailResponse.setTopic(
            TopicResponse.builder()
                .topicId(entity.getTopic().getTopicId())
                .topicName(entity.getTopic().getTopicName())
                .build()
        );
        questionDetailResponse.setAnswers(
            entity.getAnswers().stream()
                .map(AnswerResponse::fromEntity)
                .sorted(Comparator.comparing(AnswerResponse::getDisplayOrder))
                .toList()
        );
        return questionDetailResponse;
    }
}
