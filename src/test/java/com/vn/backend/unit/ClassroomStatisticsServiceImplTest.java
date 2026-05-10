package com.vn.backend.unit;

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
import com.vn.backend.services.impl.ClassroomStatisticsServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Lớp kiểm thử cho ClassroomStatisticsServiceImpl, quản lý các unit test cho
 * chức năng thống kê lớp học.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassroomStatisticsServiceImplTest {

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private SessionExamRepository sessionExamRepository;
    @Mock
    private StudentSessionExamRepository studentSessionExamRepository;

    private ClassroomStatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();
        service = new ClassroomStatisticsServiceImpl(
                messageUtils,
                classroomRepository,
                sessionExamRepository,
                studentSessionExamRepository
        );
    }

    private Classroom activeClassroom(Long classroomId) {
        return Classroom.builder()
                .classroomId(classroomId)
                .className("SE")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .isActive(true)
                .build();
    }

    private SessionExam session(Long id, String title, LocalDateTime startDate, LocalDateTime endDate) {
        return SessionExam.builder()
                .sessionExamId(id)
                .classId(3L)
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private StudentSessionExam studentExam(Long studentId, Double score, ExamSubmissionStatus status) {
        return StudentSessionExam.builder()
                .studentId(studentId)
                .score(score)
                .submissionStatus(status)
                .build();
    }

    private void mockAverageClassroomExists(Long classroomId) {
        when(classroomRepository.findById(classroomId))
                .thenReturn(Optional.of(Classroom.builder().classroomId(classroomId).build()));
    }

    private void mockActiveClassroomExists(Long classroomId) {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(
                classroomId,
                ClassroomStatus.ACTIVE
        )).thenReturn(Optional.of(activeClassroom(classroomId)));
    }

    @Nested
    class GetAverageScoreComparisonTests {

        @Test
        @DisplayName("LH_TK_01 - Đảm bảo thống kê điểm trung bình theo ca thi hoạt động đúng với dữ liệu mock hợp lệ.")
        void getAverageScoreComparison_computesAveragePerSession() {
            SessionExam sessionExam = SessionExam.builder()
                    .sessionExamId(10L)
                    .title("Quiz 1")
                    .build();

            mockAverageClassroomExists(3L);
            when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(
                    3L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(List.of(sessionExam));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(6.0, 8.0));

            ClassroomAverageScoreComparisonResponse response = service.getAverageScoreComparison(3L);

            assertEquals(1, response.getData().size());
            assertEquals("Quiz 1", response.getData().get(0).getLabel());
            assertEquals(7.0, response.getData().get(0).getValue());
        }

        @Test
        @DisplayName("LH_TK_02 - Đảm bảo thống kê điểm trung bình ném lỗi khi classroom không tồn tại.")
        void getAverageScoreComparison_throwsWhenClassroomMissing() {
            when(classroomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getAverageScoreComparison(99L));
        }

        @Test
        @DisplayName("LH_TK_06 - Bỏ qua session không có điểm khi thống kê điểm trung bình.")
        void getAverageScoreComparison_skipsSessionWhenScoresEmpty() {
            SessionExam emptySession = SessionExam.builder()
                    .sessionExamId(10L)
                    .title("Quiz Empty")
                    .build();

            SessionExam scoredSession = SessionExam.builder()
                    .sessionExamId(11L)
                    .title("Quiz 2")
                    .build();

            mockAverageClassroomExists(3L);
            when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(
                    3L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(List.of(emptySession, scoredSession));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of());
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(9.0, 7.0));

            ClassroomAverageScoreComparisonResponse response = service.getAverageScoreComparison(3L);

            assertEquals(1, response.getData().size());
            assertEquals("Quiz 2", response.getData().get(0).getLabel());
            assertEquals(8.0, response.getData().get(0).getValue());
        }

        @Test
        @DisplayName("LH_TK_07 - Trả data rỗng khi lớp chưa có session đã nộp bài.")
        void getAverageScoreComparison_returnsEmptyDataWhenNoSessions() {
            mockAverageClassroomExists(3L);

            when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(
                    3L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(List.of());

            ClassroomAverageScoreComparisonResponse response = service.getAverageScoreComparison(3L);

            assertEquals(0, response.getData().size());
        }
    }

    @Nested
    class GetImprovementTrendTests {

        @Test
        @DisplayName("LH_TK_03 - Đảm bảo thống kê xu hướng cải thiện theo session hoạt động đúng.")
        void getImprovementTrend_buildsSessionTrendStatistics() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam first = session(10L, "Quiz 1", now.minusDays(10), now.minusDays(9));
            SessionExam second = session(11L, "Quiz 2", now.minusDays(5), now.minusDays(4));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(5.0, 7.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(8.0, 9.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(2L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(2, response.getTrendData().size());
            assertEquals(ImprovementTrend.IMPROVING, response.getOverallTrend().getTrend());
            assertEquals(6.0, response.getOverallTrend().getFirstAverageScore());
            assertEquals(8.5, response.getOverallTrend().getLastAverageScore());
        }

        @Test
        @DisplayName("LH_TK_04 - Đảm bảo thống kê xu hướng cải thiện ném lỗi khi classroom active không tồn tại.")
        void getImprovementTrend_throwsWhenActiveClassroomMissing() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(
                    99L,
                    ClassroomStatus.ACTIVE
            )).thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getImprovementTrend(
                    99L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            ));
        }

        @Test
        @DisplayName("LH_TK_05 - Trả STABLE khi không có bài thi đã hoàn thành.")
        void getImprovementTrend_returnsStableTrendWhenNoCompletedExams() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam future = session(10L, "Upcoming Quiz", now.plusDays(1), now.plusDays(2));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(future));

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(3L, null, null);

            assertEquals(0, response.getTrendData().size());
            assertEquals(ImprovementTrend.STABLE, response.getOverallTrend().getTrend());
            assertEquals(StatisticsPeriod.ALL, response.getPeriod());
            assertEquals(StatisticsGroupBy.SESSION, response.getGroupBy());
        }

        @Test
        @DisplayName("LH_TK_08 - Gom dữ liệu thống kê theo tuần.")
        void getImprovementTrend_groupsByWeek() {
            SessionExam first = session(
                    10L,
                    "Quiz Monday",
                    LocalDateTime.of(2025, 1, 6, 8, 0),
                    LocalDateTime.of(2025, 1, 6, 9, 0)
            );
            SessionExam second = session(
                    11L,
                    "Quiz Tuesday",
                    LocalDateTime.of(2025, 1, 7, 8, 0),
                    LocalDateTime.of(2025, 1, 7, 9, 0)
            );

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(6.0, 8.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(10.0));

            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(1L);
            when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
                    10L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(2L);
            when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
                    11L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(1L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.WEEK
            );

            assertEquals(1, response.getTrendData().size());

            ClassroomImprovementTrendResponse.TrendDataItem item = response.getTrendData().get(0);

            assertEquals("2025-01-06", item.getPeriod());
            assertEquals("Tuần 2025-01-06", item.getPeriodLabel());
            assertEquals(8.0, item.getAverageScore());
            assertEquals(8.0, item.getMedianScore());
            assertEquals(3, item.getTotalStudents());
            assertEquals(3, item.getSubmittedStudents());
            assertEquals(100.0, item.getPassRate());
            assertEquals(66.67, item.getExcellentRate());
        }

        @Test
        @DisplayName("LH_TK_09 - Gom dữ liệu thống kê theo tháng.")
        void getImprovementTrend_groupsByMonth() {
            SessionExam first = session(
                    10L,
                    "Quiz 1",
                    LocalDateTime.of(2025, 1, 6, 8, 0),
                    LocalDateTime.of(2025, 1, 6, 9, 0)
            );
            SessionExam second = session(
                    11L,
                    "Quiz 2",
                    LocalDateTime.of(2025, 1, 20, 8, 0),
                    LocalDateTime.of(2025, 1, 20, 9, 0)
            );

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(5.0, 7.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(9.0, 10.0));

            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(2L);
            when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
                    10L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(2L);
            when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
                    11L,
                    ExamSubmissionStatus.SUBMITTED
            )).thenReturn(2L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.MONTH
            );

            assertEquals(1, response.getTrendData().size());

            ClassroomImprovementTrendResponse.TrendDataItem item = response.getTrendData().get(0);

            assertEquals("2025-01", item.getPeriod());
            assertEquals("Tháng 2025-01", item.getPeriodLabel());
            assertEquals(7.75, item.getAverageScore());
            assertEquals(8.0, item.getMedianScore());
            assertEquals(100.0, item.getPassRate());
            assertEquals(50.0, item.getExcellentRate());
        }

        @Test
        @DisplayName("LH_TK_10 - Period MONTH chỉ lấy session trong 1 tháng gần nhất.")
        void getImprovementTrend_filtersByMonthPeriod() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam recent = session(10L, "Recent Quiz", now.minusDays(10), now.minusDays(9));
            SessionExam old = session(11L, "Old Quiz", now.minusMonths(2), now.minusMonths(2).plusHours(1));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(old, recent));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(8.0, 9.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.MONTH,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getTrendData().size());
            assertEquals("Recent Quiz", response.getTrendData().get(0).getSessionExamTitle());
            assertEquals(StatisticsPeriod.MONTH, response.getPeriod());
        }

        @Test
        @DisplayName("LH_TK_11 - Period QUARTER chỉ lấy session trong 3 tháng gần nhất.")
        void getImprovementTrend_filtersByQuarterPeriod() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam inQuarter = session(
                    10L,
                    "Quarter Quiz",
                    now.minusMonths(2),
                    now.minusMonths(2).plusHours(1)
            );
            SessionExam outOfQuarter = session(
                    11L,
                    "Old Quiz",
                    now.minusMonths(4),
                    now.minusMonths(4).plusHours(1)
            );

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(outOfQuarter, inQuarter));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(8.0, 9.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.QUARTER,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getTrendData().size());
            assertEquals("Quarter Quiz", response.getTrendData().get(0).getSessionExamTitle());
            assertEquals(StatisticsPeriod.QUARTER, response.getPeriod());
        }

        @Test
        @DisplayName("LH_TK_12 - Trả DECLINING khi điểm trung bình giảm quá ngưỡng.")
        void getImprovementTrend_returnsDecliningTrendWhenAverageDecreases() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam first = session(10L, "Quiz 1", now.minusDays(10), now.minusDays(9));
            SessionExam second = session(11L, "Quiz 2", now.minusDays(5), now.minusDays(4));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(8.0, 9.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(5.0, 6.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(2L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(ImprovementTrend.DECLINING, response.getOverallTrend().getTrend());
            assertEquals(8.5, response.getOverallTrend().getFirstAverageScore());
            assertEquals(5.5, response.getOverallTrend().getLastAverageScore());
        }

        @Test
        @DisplayName("LH_TK_13 - Trả STABLE khi mức cải thiện nằm trong ngưỡng ±1%.")
        void getImprovementTrend_returnsStableTrendWhenImprovementWithinOnePercent() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam first = session(10L, "Quiz 1", now.minusDays(10), now.minusDays(9));
            SessionExam second = session(11L, "Quiz 2", now.minusDays(5), now.minusDays(4));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(7.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(7.05));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(1L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(1L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(ImprovementTrend.STABLE, response.getOverallTrend().getTrend());
            assertEquals(0.71, response.getOverallTrend().getImprovement());
        }

        @Test
        @DisplayName("LH_TK_14 - Tính median đúng khi số lượng điểm là số lẻ.")
        void getImprovementTrend_calculatesMedianForOddNumberOfScores() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam exam = session(10L, "Quiz", now.minusDays(10), now.minusDays(9));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(exam));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(9.0, 5.0, 7.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(3L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getTrendData().size());
            assertEquals(7.0, response.getTrendData().get(0).getMedianScore());
        }

        @Test
        @DisplayName("LH_TK_15 - Tính median đúng khi số lượng điểm là số chẵn.")
        void getImprovementTrend_calculatesMedianForEvenNumberOfScores() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam exam = session(10L, "Quiz", now.minusDays(10), now.minusDays(9));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(exam));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(5.0, 7.0, 9.0, 10.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(4L);

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getTrendData().size());
            assertEquals(8.0, response.getTrendData().get(0).getMedianScore());
        }

        @Test
        @DisplayName("LH_TK_16 - Tính đúng số sinh viên cải thiện liên tục và giảm sút.")
        void getImprovementTrend_countsConsistentImproversAndDecliningStudents() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam first = session(10L, "Quiz 1", now.minusDays(10), now.minusDays(9));
            SessionExam second = session(11L, "Quiz 2", now.minusDays(5), now.minusDays(4));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(5.0, 9.0, 5.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(7.0, 6.0, 5.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(3L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(3L);

            when(studentSessionExamRepository.findAllBySessionExamId(10L))
                    .thenReturn(List.of(
                            studentExam(1L, 5.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(2L, 9.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(3L, 5.0, ExamSubmissionStatus.SUBMITTED)
                    ));
            when(studentSessionExamRepository.findAllBySessionExamId(11L))
                    .thenReturn(List.of(
                            studentExam(1L, 7.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(2L, 6.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(3L, 5.0, ExamSubmissionStatus.SUBMITTED)
                    ));

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getStatistics().getConsistentImprovers());
            assertEquals(1, response.getStatistics().getDecliningStudents());
        }

        @Test
        @DisplayName("LH_TK_17 - Bỏ qua score null và bài chưa SUBMITTED khi tính thống kê theo sinh viên.")
        void getImprovementTrend_ignoresNullScoreAndNonSubmittedExamInStudentStatistics() {
            LocalDateTime now = LocalDateTime.now();

            SessionExam first = session(10L, "Quiz 1", now.minusDays(10), now.minusDays(9));
            SessionExam second = session(11L, "Quiz 2", now.minusDays(5), now.minusDays(4));

            mockActiveClassroomExists(3L);
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                    .thenReturn(List.of(first, second));

            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                    .thenReturn(List.of(5.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                    .thenReturn(List.of(7.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(3L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(3L);

            when(studentSessionExamRepository.findAllBySessionExamId(10L))
                    .thenReturn(List.of(
                            studentExam(1L, 5.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(2L, 10.0, null),
                            studentExam(3L, null, ExamSubmissionStatus.SUBMITTED)
                    ));
            when(studentSessionExamRepository.findAllBySessionExamId(11L))
                    .thenReturn(List.of(
                            studentExam(1L, 7.0, ExamSubmissionStatus.SUBMITTED),
                            studentExam(2L, 6.0, ExamSubmissionStatus.SUBMITTED)
                    ));

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L,
                    StatisticsPeriod.ALL,
                    StatisticsGroupBy.SESSION
            );

            assertEquals(1, response.getStatistics().getConsistentImprovers());
            assertEquals(0, response.getStatistics().getDecliningStudents());
        }
    }
}
