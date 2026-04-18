package com.vn.backend.dto.response.studentsessionexam;

import com.vn.backend.dto.redis.ExamQuestionAnswerDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQuestionAnswerResponse {

  private Long id;
  private String answerContent;
  private Integer displayOrder;
  private Boolean isCorrect;

  public static ExamQuestionAnswerResponse mapToExamQuestionAnswerResponse(
      ExamQuestionAnswerDTO answer) {
    if (answer == null) {
      return null;
    }

    return ExamQuestionAnswerResponse.builder()
        .id(answer.getId())
        .answerContent(answer.getAnswerContent())
        .displayOrder(answer.getDisplayOrder())
        .isCorrect(
            answer.getIsCorrect()
        )
        .build();
  }
}
