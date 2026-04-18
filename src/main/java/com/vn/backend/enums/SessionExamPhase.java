package com.vn.backend.enums;

public enum SessionExamPhase {
  NOT_STARTED,   // countdownStartAt == null
  COUNTDOWN,     // countdownStartAt <= now < readyAt
  ONGOING,       // readyAt <= now < examEndAt
  ENDED          // now >= examEndAt
}
