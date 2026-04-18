package com.vn.backend.dto.response.exam;

import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.entities.ExamQuestion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQuestionsSearchResponse {

  private Long examId;
  private Integer orderIndex;
  private Double score;
  private QuestionDetailResponse question;

  public static ExamQuestionsSearchResponse fromEntity(ExamQuestion entity) {
    return ExamQuestionsSearchResponse.builder()
        .examId(entity.getExamId())
        .orderIndex(entity.getOrderIndex())
        .score(entity.getScore())
        .question(
            QuestionDetailResponse.fromEntity(entity.getQuestion())
        )
        .build();
  }
}
