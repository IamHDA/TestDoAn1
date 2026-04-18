package com.vn.backend.dto.request.question;

import com.vn.backend.enums.QuestionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionAvailableSearchRequestDTO {

  private Long examId;
  private Long subjectId;
  private Long topicId;
  private Integer difficultyLevel;
  private QuestionType type;
  private String content;
  private Long createdBy;
}
