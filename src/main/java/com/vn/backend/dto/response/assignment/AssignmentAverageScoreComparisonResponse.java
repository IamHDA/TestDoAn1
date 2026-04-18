package com.vn.backend.dto.response.assignment;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentAverageScoreComparisonResponse {

  private List<AverageScoreItem> data;

  @Data
  @Builder
  public static class AverageScoreItem {
    private String label; // Tên bài tập (ví dụ: "Bài tập 1", "Bài tập 2")
    private Double value; // Điểm trung bình
  }
}

