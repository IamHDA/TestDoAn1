package com.vn.backend.dto.websocket;

import com.vn.backend.enums.EventType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class StudentSubmittedEvent extends SessionExamEvent {

  private LocalDateTime submittedAt;
  private Double score;

  /**
   * Factory method để tạo event
   */
  public static StudentSubmittedEvent create(
      Long sessionExamId,
      Long studentId,
      String studentCode,
      String fullName,
      String email,
      Double score,
      LocalDateTime submittedAt) {

    return StudentSubmittedEvent.builder()
        .type(EventType.STUDENT_SUBMITTED.toString())
        .sessionExamId(sessionExamId)
        .studentId(studentId)
        .studentCode(studentCode)
        .fullName(fullName)
        .email(email)
        .submittedAt(submittedAt)
        .score(score)
        .build();
  }
}

