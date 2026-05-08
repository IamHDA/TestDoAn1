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

@ExtendWith(MockitoExtension.class)
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
        service = new ClassroomStatisticsServiceImpl(messageUtils, classroomRepository,
                sessionExamRepository, studentSessionExamRepository);
    }


    @Nested
    class GetAverageScoreComparisonTests {
        @Test
        void getAverageScoreComparison_computesAveragePerSession() {
            SessionExam sessionExam = SessionExam.builder().sessionExamId(10L).title("Quiz 1").build();
            when(classroomRepository.findById(3L)).thenReturn(Optional.of(Classroom.builder().classroomId(3L).build()));
            when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(3L, ExamSubmissionStatus.SUBMITTED))
                    .thenReturn(List.of(sessionExam));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L)).thenReturn(List.of(6.0, 8.0));

            ClassroomAverageScoreComparisonResponse response = service.getAverageScoreComparison(3L);

            assertEquals(1, response.getData().size());
            assertEquals(7.0, response.getData().get(0).getValue());
        }
        @Test
        void getAverageScoreComparison_throwsWhenClassroomMissing() {
            when(classroomRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getAverageScoreComparison(99L));
        }
    }

    @Nested
    class GetImprovementTrendTests {
        @Test
        void getImprovementTrend_buildsSessionTrendStatistics() {
            Classroom classroom = Classroom.builder()
                    .classroomId(3L)
                    .className("SE")
                    .classroomStatus(ClassroomStatus.ACTIVE)
                    .isActive(true)
                    .build();
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
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(3L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L)).thenReturn(List.of(first, second));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(10L)).thenReturn(List.of(5.0, 7.0));
            when(studentSessionExamRepository.findAllScoresBySessionExamId(11L)).thenReturn(List.of(8.0, 9.0));
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(10L)).thenReturn(2L);
            when(studentSessionExamRepository.countTotalStudentsBySessionExamId(11L)).thenReturn(2L);
            when(studentSessionExamRepository.findAllBySessionExamId(10L)).thenReturn(List.of(
                    StudentSessionExam.builder().studentId(1L).score(5.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build(),
                    StudentSessionExam.builder().studentId(2L).score(7.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build()
            ));
            when(studentSessionExamRepository.findAllBySessionExamId(11L)).thenReturn(List.of(
                    StudentSessionExam.builder().studentId(1L).score(8.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build(),
                    StudentSessionExam.builder().studentId(2L).score(9.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build()
            ));

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(
                    3L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertEquals(2, response.getTrendData().size());
            assertEquals(ImprovementTrend.IMPROVING, response.getOverallTrend().getTrend());
        }
        @Test
        void getImprovementTrend_throwsWhenActiveClassroomMissing() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(99L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThrows(AppException.class, () -> service.getImprovementTrend(
                    99L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION));
        }
        @Test
        void getImprovementTrend_returnsStableTrendWhenNoCompletedExams() {
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
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(3L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(3L)).thenReturn(List.of(future));

            ClassroomImprovementTrendResponse response = service.getImprovementTrend(3L, null, null);

            assertEquals(0, response.getTrendData().size());
            assertEquals(ImprovementTrend.STABLE, response.getOverallTrend().getTrend());
            assertEquals(StatisticsPeriod.ALL, response.getPeriod());
            assertEquals(StatisticsGroupBy.SESSION, response.getGroupBy());
        }
    }
}
