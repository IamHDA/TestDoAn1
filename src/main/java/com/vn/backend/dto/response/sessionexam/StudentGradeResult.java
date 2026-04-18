package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.GradeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentGradeResult {
  private Long studentId;
  private String studentCode;
  private String fullName;
  private String email;
  private GradeStatus status; // SUCCESS, ALREADY_SUBMITTED, NOT_STARTED, ERROR
  private Double score;
  private Integer correctCount;
  private Integer totalQuestions;
  private Integer answeredCount;
  private String message;
}
