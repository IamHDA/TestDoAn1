package com.vn.backend.dto.response.question;

import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.entities.Question;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionAvailableSearchResponse {

  private Long id;
  private String content;
  private String type;
  private Integer difficultyLevel;
  private TopicResponse topic;
  private boolean added;
  private Boolean isReviewQuestion;
  private List<AnswerResponse> answers;

  public static QuestionAvailableSearchResponse fromDTO(QuestionAvailableSearchQueryDTO dto) {
    return QuestionAvailableSearchResponse.builder()
        .id(dto.getId())
        .content(dto.getContent())
        .type(dto.getType().toString())
        .difficultyLevel(dto.getDifficultyLevel())
        .topic(
            TopicResponse.builder()
                .topicId(dto.getTopic().getTopicId())
                .topicName(dto.getTopic().getTopicName())
                .build()
        )
        .added(dto.getAdded())
        .isReviewQuestion(dto.getIsReviewQuestion()) 
        .build();
  }
  public static QuestionAvailableSearchResponse fromEntity(Question entity, Set<Long> ids) {
    return QuestionAvailableSearchResponse.builder()
        .id(entity.getQuestionId())
        .content(entity.getContent())
        .type(entity.getType().toString())
        .difficultyLevel(entity.getDifficultyLevel())
        .topic(
            TopicResponse.builder()
                .topicId(entity.getTopic().getTopicId())
                .topicName(entity.getTopic().getTopicName())
                .build()
        )
        .added(ids.contains(entity.getQuestionId()))
        .isReviewQuestion(entity.getIsReviewQuestion())
        .answers(
            entity.getAnswers().stream()
                .map(AnswerResponse::fromEntity)
                .toList()
        )
        .build();
  }
}
