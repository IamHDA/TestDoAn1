package com.vn.backend.dto.request.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQuestionsDeleteRequestDTO {

  private Long examId;
  private Long questionId;
}
