package com.vn.backend.dto.redis;

import com.vn.backend.constants.AppConst;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.enums.SessionExamPhase;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionExamStateDTO {
  private Long instructorId;
  private Long sessionExamId;
  private String title;
  private String description;
  private QuestionOrderMode questionOrderMode;
  private LocalDateTime countdownStartAt;
  private Long duration; // minutes
  private boolean isInstantlyResult;
  private int totalStudents;
  private int joinedCount;
  private int downloadedCount;
  private int submittedCount;
  private int violationCount;

  public LocalDateTime getReadyAt() {
    return countdownStartAt.plusSeconds(AppConst.COUNTDOWN_S);
  }

  public LocalDateTime getExamEndAt() {
    return this.getReadyAt().plusMinutes(duration);
  }

  public SessionExamPhase computePhase() {
    if (countdownStartAt == null || duration == null) {
      return SessionExamPhase.NOT_STARTED;
    }

    LocalDateTime now = LocalDateTime.now();

    if (now.isBefore(this.getReadyAt())) {
      return SessionExamPhase.COUNTDOWN;
    } else if (now.isBefore(this.getExamEndAt())) {
      return SessionExamPhase.ONGOING;
    } else {
      return SessionExamPhase.ENDED;
    }
  }
}


