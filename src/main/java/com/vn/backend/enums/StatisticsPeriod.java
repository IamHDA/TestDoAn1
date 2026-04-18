package com.vn.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatisticsPeriod {
  ALL,        // Tất cả thời gian
  MONTH,      // 1 tháng gần nhất
  QUARTER,    // 3 tháng gần nhất
  SEMESTER;   // 6 tháng gần nhất

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static StatisticsPeriod from(String value) {
    if (value == null || value.isBlank()) {
      return ALL; // Default
    }
    try {
      return StatisticsPeriod.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ALL; // Default if invalid
    }
  }
}

