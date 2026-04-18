package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.GradeCalculationMethod;
import com.vn.backend.enums.LateSubmissionPolicy;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSettingUpdateRequestDTO {

  private boolean allowStudentPost;

//  private boolean allowStudentComment;
//
//  private GradeCalculationMethod gradeCalculationMethod;
//
//  private LateSubmissionPolicy lateSubmissionPolicy;
//
  private boolean notifyEmail;
//
//  private boolean notifyInApp;

}
