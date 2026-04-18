package com.vn.backend.dto.response.studentsessionexam;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentExamResultResponse {

  private Long studentSessionExamId;
  private Double score;
  private List<StudentExamQuestionResponse> questions;
  private Map<Long, List<Long>> studentAnswers;
}
