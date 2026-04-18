package com.vn.backend.dto.request.assignment;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import lombok.Data;

@Data
public class AssigneeAddRequest {

  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.USER_ID)
  private String userId;

  public AssigneeAddRequestDTO toDTO(){
    return AssigneeAddRequestDTO.builder()
        .userId(Long.parseLong(this.userId))
        .build();
  }
}
