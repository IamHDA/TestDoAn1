package com.vn.backend.dto.request.question;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.utils.EnumUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class QuestionAvailableSearchRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
  private String examId;

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SUBJECT_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.SUBJECT_ID)
  private String subjectId;

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.TOPIC_ID)
  private String topicId;

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.DIFFICULTY_LEVEL)
  private String difficultyLevel;

  @ValidEnum(enumClass = QuestionType.class, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.TYPE)
  private String type;

  private String search;

  public QuestionAvailableSearchRequestDTO toDTO() {
    return QuestionAvailableSearchRequestDTO.builder()
        .examId(Long.parseLong(this.examId))
        .subjectId(Long.parseLong(this.subjectId))
        .topicId(
            (this.topicId == null || this.topicId.isEmpty()) ? null : Long.parseLong(this.topicId))
        .difficultyLevel(Integer.getInteger(this.difficultyLevel))
        .type(EnumUtils.fromString(QuestionType.class, this.type))
        .content(SearchUtils.getLikeValue(this.search))
        .build();
  }
}
