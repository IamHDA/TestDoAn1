package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.ClassCodeStatus;
import com.vn.backend.enums.ClassroomStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassroomUpdateRequestDTO {

  private String className;

  private String description;

  private String coverImageUrl;

  private ClassroomStatus classroomStatus;

  private ClassCodeStatus classCodeStatus;

  private List<ClassScheduleRequestDTO> classScheduleRequestDTOS;
}
