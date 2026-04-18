package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.enums.GradeCalculationMethod;
import com.vn.backend.enums.LateSubmissionPolicy;
import com.vn.backend.utils.EnumUtils;
import lombok.Data;

@Data
public class ClassroomSettingUpdateRequest {

  private boolean allowStudentPost;

//  private boolean allowStudentComment;

//  @ValidEnum(enumClass = GradeCalculationMethod.class, fieldName = FieldConst.GRADE_CALCULATION_METHOD ,message = MessageConst.VALUE_OUT_OF_RANGE)
//  private String gradeCalculationMethod;
//
//  @ValidEnum(enumClass = LateSubmissionPolicy.class, fieldName = FieldConst.LATE_SUBMISSION_POLICY ,message = MessageConst.VALUE_OUT_OF_RANGE)
//  private String lateSubmissionPolicy;

  private boolean notifyEmail;
//
//  private boolean notifyInApp;

  public ClassroomSettingUpdateRequestDTO toDTO() {
    return ClassroomSettingUpdateRequestDTO.builder()
        .allowStudentPost(this.allowStudentPost)
//        .allowStudentComment(this.allowStudentComment)
//        .gradeCalculationMethod(EnumUtils.fromString(GradeCalculationMethod.class ,this.gradeCalculationMethod))
//        .lateSubmissionPolicy(EnumUtils.fromString(LateSubmissionPolicy.class ,this.lateSubmissionPolicy))
        .notifyEmail(this.notifyEmail)
//        .notifyInApp(this.notifyInApp)
        .build();
  }
}
