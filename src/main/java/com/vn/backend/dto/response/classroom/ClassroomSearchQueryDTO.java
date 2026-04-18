package com.vn.backend.dto.response.classroom;

import com.vn.backend.entities.Subject;
import com.vn.backend.enums.ClassroomStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomSearchQueryDTO {

  private Long classroomId;
  private String className;
  private Subject subject;
  private String coverImageUrl;
  private ClassroomStatus classroomStatus;

  // teacher
  private Long teacherId;
  private String userName;
  private String fullName;
  private String avatarUrl;

  private Long memberCount;
  private Long assignmentCount;

  private LocalDateTime createdAt;
}
