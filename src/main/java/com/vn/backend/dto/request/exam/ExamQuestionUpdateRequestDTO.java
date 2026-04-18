package com.vn.backend.dto.request.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQuestionUpdateRequestDTO {
  private Long examId;
  private Long questionId;
  private Integer orderIndex;
  private Double score;
}
