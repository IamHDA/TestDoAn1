package com.vn.backend.dto.websocket;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ExamStartingEvent extends SessionExamEvent {

  private LocalDateTime countdownStartAt;
  private LocalDateTime readyAt;
  private LocalDateTime examEndAt;
  private Long duration; // minutes
}