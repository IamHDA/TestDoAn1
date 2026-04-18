package com.vn.backend.dto.response.classroom;

import com.vn.backend.entities.ClassroomSetting;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

@Data
public class ClassroomSettingDetailResponse {

  private Long classroomId;

  private boolean allowStudentPost;

//  private boolean allowStudentComment;
//
//  private String gradeCalculationMethod;
//
//  private String lateSubmissionPolicy;
//
  private boolean notifyEmail;
//
//  private boolean notifyInApp;

  public static ClassroomSettingDetailResponse fromEntity(ClassroomSetting entity) {
    return ModelMapperUtils.mapTo(entity, ClassroomSettingDetailResponse.class);
  }
}
