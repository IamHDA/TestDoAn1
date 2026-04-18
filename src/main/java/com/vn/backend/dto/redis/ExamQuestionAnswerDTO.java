package com.vn.backend.dto.redis;

import com.vn.backend.dto.response.examquestionanswersnapshot.ExamQuestionAnswerSnapshotResponse;
import com.vn.backend.entities.ExamQuestionAnswerSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionAnswerDTO {

  private Long id;
  private String answerContent;
  private Integer displayOrder;
  private Boolean isCorrect;

  public static ExamQuestionAnswerDTO fromResponse(ExamQuestionAnswerSnapshotResponse snapshot) {
    if (snapshot == null) return null;

    ExamQuestionAnswerDTO dto = new ExamQuestionAnswerDTO();
    dto.setId(snapshot.getId());
    dto.setAnswerContent(snapshot.getAnswerContent());
    dto.setDisplayOrder(snapshot.getDisplayOrder());
    dto.setIsCorrect(snapshot.getIsCorrect());

    return dto;
  }

  public static ExamQuestionAnswerDTO fromEntity(ExamQuestionAnswerSnapshot entity){
    if (entity == null) return null;

    ExamQuestionAnswerDTO dto = new ExamQuestionAnswerDTO();
    dto.setId(entity.getId());
    dto.setAnswerContent(entity.getAnswerContent());
    dto.setDisplayOrder(entity.getDisplayOrder());
    dto.setIsCorrect(entity.getIsCorrect());

    return dto;
  }
}
