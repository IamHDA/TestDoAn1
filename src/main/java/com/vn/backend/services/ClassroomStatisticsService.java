package com.vn.backend.services;

import com.vn.backend.dto.response.classroom.ClassroomAverageScoreComparisonResponse;
import com.vn.backend.dto.response.classroom.ClassroomImprovementTrendResponse;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;

public interface ClassroomStatisticsService {

  ClassroomAverageScoreComparisonResponse getAverageScoreComparison(Long classroomId);

  ClassroomImprovementTrendResponse getImprovementTrend(
      Long classroomId, StatisticsPeriod period, StatisticsGroupBy groupBy);
}

