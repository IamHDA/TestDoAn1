package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.utils.EnumUtils;
import lombok.Data;

@Data
public class ClassMemberStatusUpdateRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_MEMBER_STATUS)
  @ValidEnum(enumClass = ClassMemberStatus.class, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.CLASS_MEMBER_STATUS)
  private String classMemberStatus;

  public ClassMemberStatusUpdateRequestDTO toDTO() {
    return ClassMemberStatusUpdateRequestDTO.builder()
        .classMemberStatus(EnumUtils.fromString(ClassMemberStatus.class, this.classMemberStatus))
        .build();
  }
}
