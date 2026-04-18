package com.vn.backend.enums;

public enum SessionExamStatus {
  NOT_STARTED,   // countdownStartAt == null
  ONGOING,       // readyAt <= now < examEndAt
  ENDED          // now >= examEndAt
}
