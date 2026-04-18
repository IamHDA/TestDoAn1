package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.SessionExamPhase;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinExamResponse {
  private String sessionToken;
  private Long sessionExamId;
  private String title;
  private String description;
  private LocalDateTime countdownStartAt;
  private LocalDateTime readyAt;
  private LocalDateTime examEndAt;
  private Long duration;
  private SessionExamPhase phase;
  private LocalDateTime serverTime;
}
