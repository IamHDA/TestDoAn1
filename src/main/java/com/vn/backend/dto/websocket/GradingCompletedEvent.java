package com.vn.backend.dto.websocket;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class GradingCompletedEvent extends SessionExamEvent {

  private Integer totalGraded;
  private LocalDateTime completedAt;
}