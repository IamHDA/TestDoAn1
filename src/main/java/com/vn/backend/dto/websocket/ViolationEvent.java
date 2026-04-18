package com.vn.backend.dto.websocket;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ViolationEvent extends SessionExamEvent {

  // Violation details
  private String violationType;
  private Integer violationCount;
  private String description;
}