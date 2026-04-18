package com.vn.backend.dto.request.sessionexam;

import com.vn.backend.enums.ExamMode;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionExamSearchTeacherRequestDTO {

  private Long classId;
  private String title;
  private String description;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private ExamMode examMode;
  private Long createdBy;
}
