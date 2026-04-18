package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatisticsGroupBy {
  SESSION,    // Theo từng phiên thi
  WEEK,       // Theo tuần
  MONTH;      // Theo tháng

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static StatisticsGroupBy from(String value) {
    if (value == null || value.isBlank()) {
      return SESSION; // Default
    }
    try {
      return StatisticsGroupBy.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return SESSION; // Default if invalid
    }
  }
}

