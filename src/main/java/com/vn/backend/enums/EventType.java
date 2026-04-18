package com.vn.backend.enums;

public enum EventType {
  // Exam lifecycle
  EXAM_STARTING,
  EXAM_READY,
  EXAM_ENDED,

  // Student actions
  STUDENT_JOINED,
  STUDENT_DOWNLOADED,
  STUDENT_SUBMITTED,
  STUDENT_OFFLINE,

  // Monitoring
  HEARTBEAT,
  VIOLATION,

  // Grading
  GRADING_COMPLETED,
  BATCH_GRADING_COMPLETED,
}