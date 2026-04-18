package com.vn.backend.dto.request.exam;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;

@Data
public class ExamSearchRequest {

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.SUBJECT_ID)
  private String subjectId;

  private String search;

  public ExamSearchRequestDTO toDTO() {
    return ExamSearchRequestDTO.builder()
        .subjectId((this.subjectId == null || Strings.isEmpty(this.subjectId)) ? null
            : Long.parseLong(this.subjectId))
        .title(SearchUtils.getLikeValue(this.search))
        .description(SearchUtils.getLikeValue(this.search))
        .build();
  }
}
