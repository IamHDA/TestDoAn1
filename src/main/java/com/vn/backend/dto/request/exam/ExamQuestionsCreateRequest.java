package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Data;

@Data
public class ExamQuestionsCreateRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.QUESTION_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.QUESTION_ID)
  private String questionId;

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ORDER_INDEX)
  private String orderIndex;

  public ExamQuestionsCreateRequestDTO toDTO() {
    return ExamQuestionsCreateRequestDTO.builder()
        .questionId(Long.parseLong(this.questionId))
        .orderIndex(this.orderIndex == null ? null : Integer.parseInt(this.orderIndex))
        .build();
  }
}
