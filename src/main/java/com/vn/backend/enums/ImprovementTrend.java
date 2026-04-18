package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ImprovementTrend {
  IMPROVING,  // Đang cải thiện
  STABLE,     // Ổn định
  DECLINING;  // Đang giảm

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static ImprovementTrend from(String value) {
    if (value == null || value.isBlank()) {
      return STABLE; // Default
    }
    try {
      return ImprovementTrend.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return STABLE; // Default if invalid
    }
  }
}

