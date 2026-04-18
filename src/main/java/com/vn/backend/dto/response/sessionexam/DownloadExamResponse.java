package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
public class DownloadExamResponse {
  private Long sessionExamId;
  private String title;
  private String description;
  private Long duration;
  private LocalDateTime examEndAt;
  private LocalDateTime serverTime;
  private List<ExamQuestionSnapshotResponse> questions;
  private Map<Long, List<Long>> savedAnswers;
}
