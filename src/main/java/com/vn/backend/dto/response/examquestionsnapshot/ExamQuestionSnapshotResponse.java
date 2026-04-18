package com.vn.backend.dto.response.examquestionsnapshot;

import com.vn.backend.dto.redis.ExamQuestionDTO;
import com.vn.backend.dto.response.examquestionanswersnapshot.ExamQuestionAnswerSnapshotResponse;
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
public class ExamQuestionSnapshotResponse {

  private Long id;
  private Long examId;
  private String questionContent;
  private String questionImageUrl;
  private QuestionType questionType;
  private Integer difficultyLevel;
  private Long topicId;
  private Double score;
  private Integer orderIndex;
  private List<ExamQuestionAnswerSnapshotResponse> answers;
  private List<Long> selectedAnswerIds;

  public static ExamQuestionSnapshotResponse fromEntity(ExamQuestionSnapshot entity) {
    ExamQuestionSnapshotResponse response = new ExamQuestionSnapshotResponse();
    response.setId(entity.getId());
    response.setExamId(entity.getExamId());
    response.setQuestionContent(entity.getQuestionContent());
    response.setQuestionImageUrl(entity.getQuestionImageUrl());
    response.setQuestionType(entity.getQuestionType());
    response.setDifficultyLevel(entity.getDifficultyLevel());
    response.setTopicId(entity.getTopicId());
    response.setScore(entity.getScore());
    response.setOrderIndex(entity.getOrderIndex());
    response.setAnswers(
        entity.getExamQuestionAnswers().stream()
            .map(ExamQuestionAnswerSnapshotResponse::fromEntity)
            .toList()
    );
    return response;
  }

  public static ExamQuestionSnapshotResponse fromDTO(ExamQuestionDTO dto) {
    if (dto == null) {
      return null;
    }

    ExamQuestionSnapshotResponse response = new ExamQuestionSnapshotResponse();
    response.setId(dto.getId());
    response.setExamId(dto.getExamId());
    response.setQuestionContent(dto.getQuestionContent());
    response.setQuestionImageUrl(dto.getQuestionImageUrl());
    response.setQuestionType(dto.getQuestionType());
    response.setDifficultyLevel(dto.getDifficultyLevel());
    response.setTopicId(dto.getTopicId());
    response.setScore(dto.getScore());
    response.setOrderIndex(dto.getOrderIndex());

    if (dto.getAnswers() != null) {
      response.setAnswers(
          dto.getAnswers().stream()
              .map(ExamQuestionAnswerSnapshotResponse::fromDTO)
              .toList()
      );
    }

    response.setSelectedAnswerIds(dto.getSelectedAnswerIds());
    return response;
  }
}
