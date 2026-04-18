package com.vn.backend.enums;

public enum StudentExamStatus {
  NOT_JOINED,
  JOINED,
  DOWNLOADED,
  IN_PROGRESS,
  SUBMITTED;

  public static StudentExamStatus fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return StudentExamStatus.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
