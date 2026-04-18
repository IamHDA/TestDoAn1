package com.vn.backend.dto.response.classroom;

import com.vn.backend.enums.ImprovementTrend;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomImprovementTrendResponse {

  private Long classroomId;
  private String classroomName;
  private StatisticsPeriod period;
  private StatisticsGroupBy groupBy;
  private List<TrendDataItem> trendData;
  private OverallTrend overallTrend;
  private TrendStatistics statistics;

  @Data
  @Builder
  public static class TrendDataItem {
    private String period; // Date string
    private String periodLabel;
    private Long sessionExamId;
    private String sessionExamTitle;
    private Double averageScore;
    private Double medianScore;
    private Integer totalStudents;
    private Integer submittedStudents;
    private Double passRate;
    private Double excellentRate;
  }

  @Data
  @Builder
  public static class OverallTrend {
    private Double firstAverageScore;
    private Double lastAverageScore;
    private Double improvement; // Percentage
    private ImprovementTrend trend;
  }

  @Data
  @Builder
  public static class TrendStatistics {
    private Integer totalSessions;
    private Double averageImprovement;
    private Integer consistentImprovers;
    private Integer decliningStudents;
  }
}

