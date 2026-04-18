package com.vn.backend.dto.response.exam;

import com.vn.backend.dto.response.subject.SubjectResponse;
import com.vn.backend.entities.Exam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamSearchResponse {

  private Long examId;
  private String title;
  private String description;
  private SubjectResponse subject;

  public static ExamSearchResponse fromEntity(Exam entity) {
    return ExamSearchResponse.builder()
        .examId(entity.getExamId())
        .title(entity.getTitle())
        .description(entity.getDescription())
        .subject(SubjectResponse.fromEntity(entity.getSubject()))
        .build();
  }
}
