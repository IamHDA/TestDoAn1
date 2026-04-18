package com.vn.backend.dto.request.sessionexam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.enums.ExamMode;
import com.vn.backend.utils.DateUtils;
import com.vn.backend.utils.EnumUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class SessionExamSearchTeacherRequest {

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.CLASSROOM_ID)
  private String classId;

  private String search;

  private String startDate;

  private String endDate;

  @ValidEnum(enumClass = ExamMode.class, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.EXAM_MODE)
  private String examMode;

  public SessionExamSearchTeacherRequestDTO toDTO() {
    return SessionExamSearchTeacherRequestDTO.builder()
        .classId(
            (this.classId == null || this.classId.isEmpty()) ? null : Long.parseLong(this.classId))
        .title((this.search == null || this.search.isEmpty()) ? null
            : SearchUtils.getLikeValue(this.search))
        .description((this.search == null || this.search.isEmpty()) ? null
            : SearchUtils.getLikeValue(this.search))
        .startDate(DateUtils.parseLocalDateTime(this.startDate, DateUtils.YYYY_MM_DD_HH_MM))
        .endDate(DateUtils.parseLocalDateTime(this.endDate, DateUtils.YYYY_MM_DD_HH_MM))
        .examMode(
            this.examMode == null ? null : EnumUtils.fromString(ExamMode.class, this.examMode))
        .build();
  }
}
