package com.vn.backend.dto.response.sessionexam;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitExamResponse {
  private Long sessionExamId;
  private Long studentId;
  private LocalDateTime submittedAt;
  private Double score;
  private Integer correctCount;
  private Integer totalQuestions;
}
