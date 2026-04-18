package com.vn.backend.dto.response.sessionexam;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionExamDescriptiveStatisticResponse {

  private Double mean; // Điểm trung bình
  private Double median; // Trung vị
  private Double max; // Điểm cao nhất
  private Double min; // Điểm thấp nhất
  private Double mode; // Điểm xuất hiện nhiều nhất (có thể null nếu không có mode duy nhất)
  private List<ScoreDistributionItem> scoreDistribution; // Phân bố điểm
  private Integer totalStudents; // Tổng sinh viên tham gia
  private Integer submittedStudents; // Số sinh viên đã nộp bài
  private Integer notSubmittedStudents; // Số sinh viên chưa nộp bài

  @Data
  @Builder
  public static class ScoreDistributionItem {
    private String range; // Khoảng điểm, ví dụ: "0-2", "2-4", "4-6", "6-8", "8-10"
    private Integer count; // Số lượng sinh viên trong khoảng này
  }
}

