package com.vn.backend.unit;


import org.junit.jupiter.api.DisplayName;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ClassroomStatisticsServiceImplTest {

        @Mock
        private ClassroomRepository classroomRepository;
        @Mock
        private SessionExamRepository sessionExamRepository;
        @Mock
        private StudentSessionExamRepository studentSessionExamRepository;

        private ClassroomStatisticsServiceImpl service;

        /**
         * Thiết lập môi trường trước mỗi bài kiểm thử.
         */
        @BeforeEach
        void setUp() {
                MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();
                service = new ClassroomStatisticsServiceImpl(messageUtils, classroomRepository,
                                sessionExamRepository, studentSessionExamRepository);
        }

        /**
         * Các bài kiểm thử cho chức năng so sánh điểm trung bình lớp học.
         */
        @Nested
        class GetAverageScoreComparisonTests {
                @Test
                @DisplayName("LH_TK_01 - Đảm bảo thống kê điểm trung bình theo ca thi hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
                void getAverageScoreComparison_computesAveragePerSession() {
                        // Given: Có một bài thi (session) với ID 10, thuộc lớp học ID 3
                        SessionExam sessionExam = SessionExam.builder().sessionExamId(10L).title("Quiz 1").build();
                        when(classroomRepository.findById(3L))
                                        .thenReturn(Optional.of(Classroom.builder().classroomId(3L).build()));
                        when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(3L,
                                        ExamSubmissionStatus.SUBMITTED))
                                        .thenReturn(List.of(sessionExam));

                        // Giả lập điểm số của các sinh viên trong bài thi này là 6.0 và 8.0
                        when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                                        .thenReturn(List.of(6.0, 8.0));

                        // When: Lấy dữ liệu so sánh điểm trung bình
                        ClassroomAverageScoreComparisonResponse response = service.getAverageScoreComparison(3L);

                        // Then: Kết quả trả về phải chứa 1 bài thi và điểm trung bình là (6+8)/2 = 7.0
                        assertEquals(1, response.getData().size());
                        assertEquals(7.0, response.getData().get(0).getValue());
                }

                @Test

                @DisplayName("LH_TK_02 - Đảm bảo thống kê điểm trung bình theo ca thi xử lý đúng trường hợp lỗi: throws when classroom missing.")
                void getAverageScoreComparison_throwsWhenClassroomMissing() {
                        when(classroomRepository.findById(99L)).thenReturn(Optional.empty());

                        assertThrows(AppException.class, () -> service.getAverageScoreComparison(99L));
                }
        }

        /**
         * Các bài kiểm thử cho chức năng lấy xu hướng cải thiện điểm số lớp học.
         */
        @Nested
        class GetImprovementTrendTests {
                @Test
                @DisplayName("LH_TK_03 - Đảm bảo thống kê xu hướng cải thiện hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
                void getImprovementTrend_buildsSessionTrendStatistics() {
                        // Given: Lớp học ID 3 đang hoạt động
                        Classroom classroom = Classroom.builder()
                                        .classroomId(3L)
                                        .className("SE")
                                        .classroomStatus(ClassroomStatus.ACTIVE)
                                        .isActive(true)
                                        .build();

                        // Có 2 bài thi: Quiz 1 (10 ngày trước) và Quiz 2 (5 ngày trước)
                        SessionExam first = SessionExam.builder()
                                        .sessionExamId(10L)
                                        .classId(3L)
                                        .title("Quiz 1")
                                        .startDate(LocalDateTime.now().minusDays(10))
                                        .endDate(LocalDateTime.now().minusDays(9))
                                        .build();
                        SessionExam second = SessionExam.builder()
                                        .sessionExamId(11L)
                                        .classId(3L)
                                        .title("Quiz 2")
                                        .startDate(LocalDateTime.now().minusDays(5))
                                        .endDate(LocalDateTime.now().minusDays(4))
                                        .build();

                        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(3L,
                                        ClassroomStatus.ACTIVE))
                                        .thenReturn(Optional.of(classroom));
                        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                                        .thenReturn(List.of(first, second));

                        // Giả lập điểm số: Quiz 1 (5.0, 7.0 -> TB 6.0), Quiz 2 (8.0, 9.0 -> TB 8.5)
                        when(studentSessionExamRepository.findAllScoresBySessionExamId(10L))
                                        .thenReturn(List.of(5.0, 7.0));
                        when(studentSessionExamRepository.findAllScoresBySessionExamId(11L))
                                        .thenReturn(List.of(8.0, 9.0));
                        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
                        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(2L);

                        // When: Lấy xu hướng cải thiện điểm số theo từng bài thi (SESSION)
                        ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                                        3L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

                        // Then: Phải có dữ liệu cho cả 2 bài thi và xu hướng chung là IMPROVING (do 8.5
                        // > 6.0)
                        assertEquals(2, response.getTrendData().size());
                        assertEquals(ImprovementTrend.IMPROVING, response.getOverallTrend().getTrend());
                }

                @Test

                @DisplayName("LH_TK_04 - Đảm bảo thống kê xu hướng cải thiện xử lý đúng trường hợp lỗi: throws when active classroom missing.")
                void getImprovementTrend_throwsWhenActiveClassroomMissing() {
                        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(99L,
                                        ClassroomStatus.ACTIVE))
                                        .thenReturn(Optional.empty());

                        assertThrows(AppException.class, () -> service.getImprovementTrend(
                                        99L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION));
                }

                @Test

                @DisplayName("LH_TK_05 - Đảm bảo thống kê xu hướng cải thiện hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
                void getImprovementTrend_returnsStableTrendWhenNoCompletedExams() {
                        // Given: Lớp học có 1 bài thi sắp tới (chưa diễn ra)
                        Classroom classroom = Classroom.builder()
                                        .classroomId(3L)
                                        .className("SE")
                                        .classroomStatus(ClassroomStatus.ACTIVE)
                                        .isActive(true)
                                        .build();
                        SessionExam future = SessionExam.builder()
                                        .sessionExamId(10L)
                                        .classId(3L)
                                        .title("Upcoming Quiz")
                                        .startDate(LocalDateTime.now().plusDays(1))
                                        .endDate(LocalDateTime.now().plusDays(2))
                                        .build();
                        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(3L,
                                        ClassroomStatus.ACTIVE))
                                        .thenReturn(Optional.of(classroom));
                        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L))
                                        .thenReturn(List.of(future));

                        // When: Lấy xu hướng cải thiện
                        ClassroomImprovementTrendResponse response = service.getImprovementTrend(3L, null, null);

                        // Then: Xu hướng mặc định là STABLE vì không có bài thi nào đã hoàn thành để tính toán
                        assertEquals(0, response.getTrendData().size());
                        assertEquals(ImprovementTrend.STABLE, response.getOverallTrend().getTrend());
                        assertEquals(StatisticsPeriod.ALL, response.getPeriod());
                        assertEquals(StatisticsGroupBy.SESSION, response.getGroupBy());
                }
        }
}
