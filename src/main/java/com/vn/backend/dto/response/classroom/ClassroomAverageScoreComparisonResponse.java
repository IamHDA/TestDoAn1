package com.vn.backend.dto.response.classroom;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomAverageScoreComparisonResponse {

  private List<AverageScoreItem> data;

  @Data
  @Builder
  public static class AverageScoreItem {
    private String label; // Tên bài thi (ví dụ: "Giữa kỳ", "Cuối kỳ")
    private Double value; // Điểm trung bình
  }
}

