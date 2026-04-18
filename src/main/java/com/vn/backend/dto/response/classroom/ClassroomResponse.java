package com.vn.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomResponse {

  private Long classroomId;
  private String className;
}
