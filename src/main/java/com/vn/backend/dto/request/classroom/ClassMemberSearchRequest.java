package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.utils.EnumUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class ClassMemberSearchRequest {

  @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASSROOM_ID)
  @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT)
  private String classroomId;

  private String keyword;

  @ValidEnum(enumClass = ClassMemberRole.class, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.CLASS_MEMBER_ROLE)
  private String classMemberRole;

  @ValidEnum(enumClass = ClassMemberStatus.class, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.CLASS_MEMBER_STATUS)
  private String classMemberStatus;

  public ClassMemberSearchRequestDTO toDTO(){
    return ClassMemberSearchRequestDTO.builder()
        .classroomId(Long.parseLong(this.classroomId))
        .keyword(SearchUtils.getLikeValue(this.keyword))
        .classMemberRole(EnumUtils.fromString(ClassMemberRole.class, this.classMemberRole))
        .classMemberStatus(EnumUtils.fromString(ClassMemberStatus.class, this.classMemberStatus))
        .build();
  }
}
