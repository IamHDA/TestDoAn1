package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.enums.SessionExamPhase;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionExamMonitoringResponse {
  private Long sessionExamId;
  private String title;
  private String description;
  private SessionExamPhase phase;

  private LocalDateTime countdownStartAt;
  private LocalDateTime readyAt;
  private LocalDateTime examEndAt;
  private Long duration;

  private Integer totalStudents;
  private Integer joinedCount;
  private Integer downloadedCount;
  private Integer submittedCount;
  private Integer violationCount;
  private Integer activeCount;

  private List<StudentStateInfo> students;
  private List<ActivityHistoryResponse> activityHistories;
  private LocalDateTime serverTime;
}
