package com.vn.backend.dto.websocket;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
public class SessionExamEvent {

  private String type;
  private Long sessionExamId;
  // Student info
  private Long studentId;
  private String studentCode;
  private String fullName;
  private String email;

  @Builder.Default
  private String timestamp = LocalDateTime.now().toString();
}
