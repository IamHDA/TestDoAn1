package com.vn.backend.services.impl;

import com.vn.backend.dto.response.classroom.ClassroomAverageScoreComparisonResponse;
import com.vn.backend.dto.response.classroom.ClassroomImprovementTrendResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.services.ClassroomStatisticsService;
import com.vn.backend.utils.MessageUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vn.backend.constants.AppConst.MessageConst;

@Service
public class ClassroomStatisticsServiceImpl extends BaseService implements ClassroomStatisticsService {

  private final ClassroomRepository classroomRepository;
  private final SessionExamRepository sessionExamRepository;
  private final StudentSessionExamRepository studentSessionExamRepository;

  public ClassroomStatisticsServiceImpl(
      MessageUtils messageUtils,
      ClassroomRepository classroomRepository,
      SessionExamRepository sessionExamRepository,
      StudentSessionExamRepository studentSessionExamRepository) {
    super(messageUtils);
    this.classroomRepository = classroomRepository;
    this.sessionExamRepository = sessionExamRepository;
    this.studentSessionExamRepository = studentSessionExamRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public ClassroomAverageScoreComparisonResponse getAverageScoreComparison(Long classroomId) {
    classroomRepository.findById(classroomId)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

    List<SessionExam> sessionExams = sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(
        classroomId, ExamSubmissionStatus.SUBMITTED);

    List<ClassroomAverageScoreComparisonResponse.AverageScoreItem> items = new ArrayList<>();
    for (SessionExam sessionExam : sessionExams) {
      List<Double> scores = studentSessionExamRepository.findAllScoresBySessionExamId(
          Long.valueOf(sessionExam.getSessionExamId()));

      if (!scores.isEmpty()) {
        double average = scores.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        items.add(ClassroomAverageScoreComparisonResponse.AverageScoreItem.builder()
            .label(sessionExam.getTitle())
            .value(average)
            .build());
      }
    }

    return ClassroomAverageScoreComparisonResponse.builder()
        .data(items)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public ClassroomImprovementTrendResponse getImprovementTrend(
      Long classroomId, StatisticsPeriod period, StatisticsGroupBy groupBy) {
    // Validate classroom
    Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(classroomId, ClassroomStatus.ACTIVE)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

    // Set defaults
    if (period == null) {
      period = StatisticsPeriod.ALL;
    }
    if (groupBy == null) {
      groupBy = StatisticsGroupBy.SESSION;
    }

    // Calculate date range based on period
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime periodStartDate = null;
    if (period == StatisticsPeriod.MONTH) {
      periodStartDate = now.minusMonths(1);
    } else if (period == StatisticsPeriod.QUARTER) {
      periodStartDate = now.minusMonths(3);
    } else if (period == StatisticsPeriod.SEMESTER) {
      periodStartDate = now.minusMonths(6);
    }
    final LocalDateTime finalStartDate = periodStartDate;

    // Get session exams
    List<SessionExam> sessionExams = sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(classroomId);
    
    // Filter by period and endDate < now (only completed exams)
    List<SessionExam> filteredExams = sessionExams.stream()
        .filter(se -> se.getEndDate().isBefore(now))
        .filter(se -> finalStartDate == null || se.getEndDate().isAfter(finalStartDate) || se.getEndDate().isEqual(finalStartDate))
        .sorted(Comparator.comparing(SessionExam::getStartDate))
        .collect(Collectors.toList());

    // Group data based on groupBy
    List<ClassroomImprovementTrendResponse.TrendDataItem> trendData = groupTrendData(
        filteredExams, groupBy);

    // Calculate overall trend
    ClassroomImprovementTrendResponse.OverallTrend overallTrend = calculateOverallTrend(trendData);

    // Calculate statistics
    ClassroomImprovementTrendResponse.TrendStatistics statistics = calculateTrendStatistics(
        filteredExams, trendData);

    return ClassroomImprovementTrendResponse.builder()
        .classroomId(classroomId)
        .classroomName(classroom.getClassName())
        .period(period)
        .groupBy(groupBy)
        .trendData(trendData)
        .overallTrend(overallTrend)
        .statistics(statistics)
        .build();
  }

  private List<ClassroomImprovementTrendResponse.TrendDataItem> groupTrendData(
      List<SessionExam> sessionExams, StatisticsGroupBy groupBy) {
    if (groupBy == StatisticsGroupBy.SESSION) {
      return sessionExams.stream()
          .map(this::createTrendDataItemFromSession)
          .filter(item -> item != null)
          .collect(Collectors.toList());
    } else if (groupBy == StatisticsGroupBy.WEEK) {
      return groupByWeek(sessionExams);
    } else if (groupBy == StatisticsGroupBy.MONTH) {
      return groupByMonth(sessionExams);
    }
    // Default to session
    return sessionExams.stream()
        .map(this::createTrendDataItemFromSession)
        .filter(item -> item != null)
        .collect(Collectors.toList());
  }

  private ClassroomImprovementTrendResponse.TrendDataItem createTrendDataItemFromSession(
      SessionExam sessionExam) {
    List<Double> scores = studentSessionExamRepository.findAllScoresBySessionExamId(
        sessionExam.getSessionExamId());

    if (scores.isEmpty()) {
      return null;
    }

    // Calculate statistics
    double averageScore = scores.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    double medianScore = calculateMedian(scores);

    int totalStudents = scores.size();
    long submittedStudents = scores.size(); // All scores are from submitted exams

    // Calculate pass rate (>= 4.0) and excellent rate (>= 8.0)
    long passCount = scores.stream().filter(s -> s >= 4.0).count();
    long excellentCount = scores.stream().filter(s -> s >= 8.0).count();
    double passRate = totalStudents > 0 ? (double) passCount / totalStudents * 100 : 0.0;
    double excellentRate = totalStudents > 0 ? (double) excellentCount / totalStudents * 100 : 0.0;

    // Get total students in classroom for this session
    Long totalStudentsInClass = studentSessionExamRepository.countTotalStudentsBySessionExamId(
        sessionExam.getSessionExamId());

    return ClassroomImprovementTrendResponse.TrendDataItem.builder()
        .period(sessionExam.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
        .periodLabel(sessionExam.getTitle())
        .sessionExamId(sessionExam.getSessionExamId())
        .sessionExamTitle(sessionExam.getTitle())
        .averageScore(roundToTwoDecimals(averageScore))
        .medianScore(roundToTwoDecimals(medianScore))
        .totalStudents(totalStudentsInClass != null ? totalStudentsInClass.intValue() : totalStudents)
        .submittedStudents((int) submittedStudents)
        .passRate(roundToTwoDecimals(passRate))
        .excellentRate(roundToTwoDecimals(excellentRate))
        .build();
  }

  private List<ClassroomImprovementTrendResponse.TrendDataItem> groupByWeek(
      List<SessionExam> sessionExams) {
    Map<String, List<SessionExam>> groupedByWeek = new LinkedHashMap<>();

    for (SessionExam se : sessionExams) {
      LocalDateTime startDate = se.getStartDate();
      // Get start of week (Monday)
      LocalDateTime weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
      String weekKey = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE);

      groupedByWeek.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(se);
    }

    return groupedByWeek.entrySet().stream()
        .map(entry -> createTrendDataItemFromSessions(entry.getKey(), 
            "Tuần " + entry.getKey(), entry.getValue()))
        .filter(item -> item != null)
        .collect(Collectors.toList());
  }

  private List<ClassroomImprovementTrendResponse.TrendDataItem> groupByMonth(
      List<SessionExam> sessionExams) {
    Map<String, List<SessionExam>> groupedByMonth = new LinkedHashMap<>();

    for (SessionExam se : sessionExams) {
      LocalDateTime startDate = se.getStartDate();
      String monthKey = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

      groupedByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(se);
    }

    return groupedByMonth.entrySet().stream()
        .map(entry -> createTrendDataItemFromSessions(entry.getKey(),
            "Tháng " + entry.getKey(), entry.getValue()))
        .filter(item -> item != null)
        .collect(Collectors.toList());
  }

  private ClassroomImprovementTrendResponse.TrendDataItem createTrendDataItemFromSessions(
      String period, String periodLabel, List<SessionExam> sessionExams) {
    List<Double> allScores = new ArrayList<>();
    int totalStudents = 0;
    int submittedStudents = 0;

    for (SessionExam se : sessionExams) {
      List<Double> scores = studentSessionExamRepository.findAllScoresBySessionExamId(
          se.getSessionExamId());
      allScores.addAll(scores);
      
      Long total = studentSessionExamRepository.countTotalStudentsBySessionExamId(
          se.getSessionExamId());
      if (total != null) {
        totalStudents += total.intValue();
      }
      
      Long submitted = studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
          se.getSessionExamId(), ExamSubmissionStatus.SUBMITTED);
      if (submitted != null) {
        submittedStudents += submitted.intValue();
      }
    }

    if (allScores.isEmpty()) {
      return null;
    }

    double averageScore = allScores.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    double medianScore = calculateMedian(allScores);

    long passCount = allScores.stream().filter(s -> s >= 5.0).count();
    long excellentCount = allScores.stream().filter(s -> s >= 8.0).count();
    double passRate = allScores.size() > 0 ? (double) passCount / allScores.size() * 100 : 0.0;
    double excellentRate = allScores.size() > 0 ? (double) excellentCount / allScores.size() * 100 : 0.0;

    return ClassroomImprovementTrendResponse.TrendDataItem.builder()
        .period(period)
        .periodLabel(periodLabel)
        .sessionExamId(null) // Multiple sessions
        .sessionExamTitle(periodLabel)
        .averageScore(roundToTwoDecimals(averageScore))
        .medianScore(roundToTwoDecimals(medianScore))
        .totalStudents(totalStudents)
        .submittedStudents(submittedStudents)
        .passRate(roundToTwoDecimals(passRate))
        .excellentRate(roundToTwoDecimals(excellentRate))
        .build();
  }

  private ClassroomImprovementTrendResponse.OverallTrend calculateOverallTrend(
      List<ClassroomImprovementTrendResponse.TrendDataItem> trendData) {
    if (trendData.isEmpty()) {
      return ClassroomImprovementTrendResponse.OverallTrend.builder()
          .firstAverageScore(0.0)
          .lastAverageScore(0.0)
          .improvement(0.0)
          .trend(ImprovementTrend.STABLE)
          .build();
    }

    double firstScore = trendData.get(0).getAverageScore();
    double lastScore = trendData.get(trendData.size() - 1).getAverageScore();
    
    double improvement = firstScore > 0 
        ? ((lastScore - firstScore) / firstScore) * 100 
        : 0.0;

    ImprovementTrend trend;
    if (improvement > 1.0) {
      trend = ImprovementTrend.IMPROVING;
    } else if (improvement < -1.0) {
      trend = ImprovementTrend.DECLINING;
    } else {
      trend = ImprovementTrend.STABLE;
    }

    return ClassroomImprovementTrendResponse.OverallTrend.builder()
        .firstAverageScore(roundToTwoDecimals(firstScore))
        .lastAverageScore(roundToTwoDecimals(lastScore))
        .improvement(roundToTwoDecimals(improvement))
        .trend(trend)
        .build();
  }

  private ClassroomImprovementTrendResponse.TrendStatistics calculateTrendStatistics(
      List<SessionExam> sessionExams,
      List<ClassroomImprovementTrendResponse.TrendDataItem> trendData) {
    int totalSessions = trendData.size();

    // Calculate average improvement
    double averageImprovement = 0.0;
    if (trendData.size() > 1) {
      double totalImprovement = 0.0;
      for (int i = 1; i < trendData.size(); i++) {
        double prev = trendData.get(i - 1).getAverageScore();
        double curr = trendData.get(i).getAverageScore();
        if (prev > 0) {
          totalImprovement += ((curr - prev) / prev) * 100;
        }
      }
      averageImprovement = totalImprovement / (trendData.size() - 1);
    }

    // Calculate consistent improvers and declining students
    // This requires checking individual student progress
    int consistentImprovers = 0;
    int decliningStudents = 0;

    // Get all students in classroom
    Map<Long, List<Double>> studentScores = new HashMap<>();
    for (SessionExam se : sessionExams) {
      List<StudentSessionExam> studentExams = studentSessionExamRepository
          .findAllBySessionExamId(se.getSessionExamId());
      
      for (StudentSessionExam sse : studentExams) {
        if (sse.getScore() != null && sse.getSubmissionStatus() == ExamSubmissionStatus.SUBMITTED) {
          studentScores.computeIfAbsent(sse.getStudentId(), k -> new ArrayList<>())
              .add(sse.getScore());
        }
      }
    }

    for (List<Double> scores : studentScores.values()) {
      if (scores.size() >= 2) {
        boolean isImproving = true;
        boolean isDeclining = true;
        
        for (int i = 1; i < scores.size(); i++) {
          if (scores.get(i) < scores.get(i - 1)) {
            isImproving = false;
          }
          if (scores.get(i) > scores.get(i - 1)) {
            isDeclining = false;
          }
        }
        
        if (isImproving && !isDeclining) {
          consistentImprovers++;
        } else if (isDeclining && !isImproving) {
          decliningStudents++;
        }
      }
    }

    return ClassroomImprovementTrendResponse.TrendStatistics.builder()
        .totalSessions(totalSessions)
        .averageImprovement(roundToTwoDecimals(averageImprovement))
        .consistentImprovers(consistentImprovers)
        .decliningStudents(decliningStudents)
        .build();
  }

  private double calculateMedian(List<Double> scores) {
    if (scores.isEmpty()) {
      return 0.0;
    }
    
    List<Double> sorted = new ArrayList<>(scores);
    sorted.sort(Comparator.naturalOrder());
    
    int size = sorted.size();
    if (size % 2 == 0) {
      return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    } else {
      return sorted.get(size / 2);
    }
  }

  private double roundToTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}

