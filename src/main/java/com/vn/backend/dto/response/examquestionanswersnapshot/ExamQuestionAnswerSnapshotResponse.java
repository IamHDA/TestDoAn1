package com.vn.backend.dto.response.examquestionanswersnapshot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vn.backend.dto.redis.ExamQuestionAnswerDTO;
import com.vn.backend.entities.ExamQuestionAnswerSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionAnswerSnapshotResponse {

  private Long id;
  private String answerContent;
  private Integer displayOrder;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Boolean isCorrect;

  public static ExamQuestionAnswerSnapshotResponse fromEntity(ExamQuestionAnswerSnapshot entity) {
    ExamQuestionAnswerSnapshotResponse response = new ExamQuestionAnswerSnapshotResponse();
    response.setId(entity.getId());
    response.setAnswerContent(entity.getAnswerContent());
    response.setDisplayOrder(entity.getDisplayOrder());
    response.setIsCorrect(entity.getIsCorrect());
    return response;
  }

  public static ExamQuestionAnswerSnapshotResponse fromDTO(ExamQuestionAnswerDTO dto) {
    if (dto == null) return null;

    ExamQuestionAnswerSnapshotResponse response = new ExamQuestionAnswerSnapshotResponse();
    response.setId(dto.getId());
    response.setAnswerContent(dto.getAnswerContent());
    response.setDisplayOrder(dto.getDisplayOrder());
    return response;
  }
}
