package com.vn.backend.dto.response.sessionexam;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
public class SessionExamStartResponse {

  private Long sessionExamId;
  private String title;
  private String description;
  private LocalDateTime countdownStartAt;
  private LocalDateTime readyAt;
  private LocalDateTime examEndAt;
  private Long duration;
  private int totalStudents;
}
