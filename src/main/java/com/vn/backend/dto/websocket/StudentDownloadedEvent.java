package com.vn.backend.dto.websocket;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class StudentDownloadedEvent extends SessionExamEvent {

  private LocalDateTime downloadedAt;
  private Integer answeredCount;
}