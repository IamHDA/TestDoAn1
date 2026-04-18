package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.response.classroom.ClassroomAverageScoreComparisonResponse;
import com.vn.backend.dto.response.classroom.ClassroomImprovementTrendResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;
import com.vn.backend.services.ClassroomStatisticsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AppConst.API + "/classroom-statistics")
public class ClassroomStatisticsController extends BaseController {

  private final ClassroomStatisticsService classroomStatisticsService;

  public ClassroomStatisticsController(ClassroomStatisticsService classroomStatisticsService) {
    this.classroomStatisticsService = classroomStatisticsService;
  }

  @GetMapping("/{classroomId}/average-score-comparison")
  public AppResponse<ClassroomAverageScoreComparisonResponse> getAverageScoreComparison(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.CLASSROOM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
      String classroomId) {
    log.info("Received request to get average score comparison for classroom {}", classroomId);
    ClassroomAverageScoreComparisonResponse response = classroomStatisticsService.getAverageScoreComparison(
        Long.valueOf(classroomId));
    log.info("Successfully got average score comparison for classroom {}", classroomId);
    return success(response);
  }

  @GetMapping("/{classroomId}/improvement-trend")
  public AppResponse<ClassroomImprovementTrendResponse> getImprovementTrend(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.CLASSROOM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
      String classroomId,
      @RequestParam(required = false) String period,
      @RequestParam(required = false) String groupBy) {
    log.info("Received request to get improvement trend for classroom {} with period={}, groupBy={}", 
        classroomId, period, groupBy);
    
    // Convert String to Enum
    StatisticsPeriod periodEnum = StatisticsPeriod.from(period);
    StatisticsGroupBy groupByEnum = StatisticsGroupBy.from(groupBy);
    
    ClassroomImprovementTrendResponse response = classroomStatisticsService.getImprovementTrend(
        Long.valueOf(classroomId), periodEnum, groupByEnum);
    log.info("Successfully got improvement trend for classroom {}", classroomId);
    return success(response);
  }
}

