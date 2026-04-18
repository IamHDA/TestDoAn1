package com.vn.backend.dto.request.assignment;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class AssigneeSearchRequest {

  private String keyword;

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ASSIGNMENT_ID)
  private String assignmentId;

  public AssigneeSearchRequestDTO toDTO() {
    return AssigneeSearchRequestDTO.builder()
        .fullName(SearchUtils.getLikeValue(this.keyword))
        .username(SearchUtils.getLikeValue(this.keyword))
        .assignmentId(Long.parseLong(this.assignmentId))
        .build();
  }
}
