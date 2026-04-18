package com.vn.backend.dto.redis;

import com.vn.backend.dto.response.examquestionanswersnapshot.ExamQuestionAnswerSnapshotResponse;
import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.entities.ExamQuestionSnapshot;
import com.vn.backend.enums.QuestionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionDTO {

  private Long id;
  private Long examId;
  private String questionContent;
  private String questionImageUrl;
  private QuestionType questionType;
  private Integer difficultyLevel;
  private Long topicId;
  private Double score;
  private Integer orderIndex;
  private List<ExamQuestionAnswerDTO> answers;
  private List<Long> selectedAnswerIds;

  public static ExamQuestionDTO fromResponse(ExamQuestionSnapshotResponse response){
    if (response == null) return null;

    ExamQuestionDTO dto = new ExamQuestionDTO();
    dto.setId(response.getId());
    dto.setExamId(response.getExamId());
    dto.setQuestionContent(response.getQuestionContent());
    dto.setQuestionImageUrl(response.getQuestionImageUrl());
    dto.setQuestionType(response.getQuestionType());
    dto.setDifficultyLevel(response.getDifficultyLevel());
    dto.setTopicId(response.getTopicId());
    dto.setScore(response.getScore());
    dto.setOrderIndex(response.getOrderIndex());
    if (response.getAnswers() != null) {
      dto.setAnswers(
          response.getAnswers().stream()
              .map(ExamQuestionAnswerDTO::fromResponse)
              .toList()
      );
    }
    dto.setSelectedAnswerIds(response.getSelectedAnswerIds());
    return dto;
  }

  public static ExamQuestionDTO fromEntity(ExamQuestionSnapshot entity) {
    ExamQuestionDTO dto = new ExamQuestionDTO();
    dto.setId(entity.getId());
    dto.setExamId(entity.getExamId());
    dto.setQuestionContent(entity.getQuestionContent());
    dto.setQuestionImageUrl(entity.getQuestionImageUrl());
    dto.setQuestionType(entity.getQuestionType());
    dto.setDifficultyLevel(entity.getDifficultyLevel());
    dto.setTopicId(entity.getTopicId());
    dto.setScore(entity.getScore());
    dto.setOrderIndex(entity.getOrderIndex());
    dto.setAnswers(
        entity.getExamQuestionAnswers().stream()
            .map(ExamQuestionAnswerDTO::fromEntity)
            .toList()
    );
    return dto;
  }
}
