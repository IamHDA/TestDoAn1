package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Data;

@Data
public class ExamQuestionUpdateRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
  private String examId;

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.QUESTION_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.QUESTION_ID)
  private String questionId;

  private String orderIndex;

  private String score;

  public ExamQuestionUpdateRequestDTO toDTO() {
    return ExamQuestionUpdateRequestDTO.builder()
        .examId(Long.parseLong(this.examId))
        .questionId(Long.parseLong(this.questionId))
        .orderIndex(this.orderIndex == null ? null : Integer.parseInt(this.orderIndex))
        .score(this.orderIndex == null ? null : Double.parseDouble(this.score))
        .build();
  }
}
