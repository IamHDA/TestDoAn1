package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.StudentExamStatus;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStateInfo {
  private Long studentId;
  private String studentCode;
  private String fullName;
  private String email;

  private StudentExamStatus status;
  private Boolean active;

  private LocalDateTime joinedAt;
  private LocalDateTime downloadedAt;
  private LocalDateTime submittedAt;
  private LocalDateTime lastHeartbeatAt;

  private Integer answeredCount;

  private Integer violationCount;
  private String lastViolationType;
  private LocalDateTime lastViolationAt;

  private Double score;
  private Integer correctCount;
  private Integer totalQuestions;
}
