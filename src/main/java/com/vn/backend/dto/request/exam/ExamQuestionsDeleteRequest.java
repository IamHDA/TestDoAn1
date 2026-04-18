package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Data;

@Data
public class ExamQuestionsDeleteRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
  private String examId;

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.QUESTION_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.QUESTION_ID)
  private String questionId;

  public ExamQuestionsDeleteRequestDTO toDTO() {
    return ExamQuestionsDeleteRequestDTO.builder()
        .examId(Long.parseLong(this.examId))
        .questionId(Long.parseLong(this.questionId))
        .build();
  }
}
