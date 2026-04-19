package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.response.classroom.ClassroomAverageScoreComparisonResponse;
import com.vn.backend.dto.response.classroom.ClassroomImprovementTrendResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.ExamSubmissionStatus;
import com.vn.backend.enums.ImprovementTrend;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.services.impl.ClassroomStatisticsServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassroomStatisticsServiceImpl Unit Tests")
class ClassroomStatisticsServiceImplTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private SessionExamRepository sessionExamRepository;

    @Mock
    private StudentSessionExamRepository studentSessionExamRepository;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private ClassroomStatisticsServiceImpl classroomStatisticsService;

    private Classroom activeClassroom;
    private SessionExam sessionExam1;
    private SessionExam sessionExam2;

    @BeforeEach
    void setUp() {
        activeClassroom = Classroom.builder()
                .classroomId(100L)
                .className("Test Classroom")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .isActive(true)
                .build();

        sessionExam1 = SessionExam.builder()
                .sessionExamId(1L)
                .classId(100L)
                .title("Exam 1")
                .startDate(LocalDateTime.now().minusWeeks(2))
                .endDate(LocalDateTime.now().minusWeeks(1))
                .build();

        sessionExam2 = SessionExam.builder()
                .sessionExamId(2L)
                .classId(100L)
                .title("Exam 2")
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().minusDays(2))
                .build();
                
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");
    }

    // ===================== getAverageScoreComparison =====================

    @Test
    @DisplayName("TC_QLLH_STAT_01: getAverageScoreComparison - ném exception khi không tìm thấy lớp")
    void getAverageScoreComparison_ThrowsException_WhenClassNotFound() {
        when(classroomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomStatisticsService.getAverageScoreComparison(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("TC_QLLH_STAT_02: getAverageScoreComparison - thành công nhưng mốc thi rỗng")
    void getAverageScoreComparison_Success_EmptyExams() {
        when(classroomRepository.findById(100L)).thenReturn(Optional.of(activeClassroom));
        when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(100L, ExamSubmissionStatus.SUBMITTED))
                .thenReturn(Collections.emptyList());

        var result = classroomStatisticsService.getAverageScoreComparison(100L);

        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("TC_QLLH_STAT_03: getAverageScoreComparison - thành công và tính đúng điểm TB")
    void getAverageScoreComparison_Success_WithScores() {
        when(classroomRepository.findById(100L)).thenReturn(Optional.of(activeClassroom));
        when(sessionExamRepository.findSessionExamsWithSubmissionsByClassroomId(100L, ExamSubmissionStatus.SUBMITTED))
                .thenReturn(Arrays.asList(sessionExam1, sessionExam2));

        // Điểm của exam 1: 5.0, 7.0 (TB=6.0)
        when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(Arrays.asList(5.0, 7.0));
        // Điểm của exam 2: rỗng => không có trong dataset
        when(studentSessionExamRepository.findAllScoresBySessionExamId(2L)).thenReturn(Collections.emptyList());

        var result = classroomStatisticsService.getAverageScoreComparison(100L);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getLabel()).isEqualTo("Exam 1");
        assertThat(result.getData().get(0).getValue()).isEqualTo(6.0);
    }

    // ===================== getImprovementTrend =====================

    @Test
    @DisplayName("TC_QLLH_STAT_04: getImprovementTrend - ném exception khi lớp không active")
    void getImprovementTrend_ThrowsException_WhenClassInactive() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(999L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomStatisticsService.getImprovementTrend(999L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("TC_QLLH_STAT_05: getImprovementTrend - thành công, groupBy SESSION, xu hướng STABLE (rỗng dữ liệu)")
    void getImprovementTrend_Success_GroupBySession_EmptyScores() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));
        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                .thenReturn(List.of(sessionExam1));
        
        when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(Collections.emptyList());
        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(1L)).thenReturn(10L);
        when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(Collections.emptyList());

        // Test trường hợp truyền param null, hệ thống tự fallback period.ALL và groupBy.SESSION
        var result = classroomStatisticsService.getImprovementTrend(100L, null, null); 
        
        assertThat(result.getPeriod()).isEqualTo(StatisticsPeriod.ALL);
        assertThat(result.getGroupBy()).isEqualTo(StatisticsGroupBy.SESSION);
        assertThat(result.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.STABLE);
        assertThat(result.getTrendData()).isEmpty();
    }

    @Test
    @DisplayName("TC_QLLH_STAT_06: getImprovementTrend - xu hướng IMPROVING")
    void getImprovementTrend_Success_Improving() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));
        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                .thenReturn(Arrays.asList(sessionExam1, sessionExam2));
        
        // Setup scores for Exam 1 (avg: 5.0)
        when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(Arrays.asList(4.0, 6.0));
        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(1L)).thenReturn(2L);
        when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(1L, ExamSubmissionStatus.SUBMITTED)).thenReturn(2L);
        
        // Setup scores for Exam 2 (avg: 8.0)
        when(studentSessionExamRepository.findAllScoresBySessionExamId(2L)).thenReturn(Arrays.asList(7.0, 9.0));
        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(2L)).thenReturn(2L);
        
        // Setup Student exams để test declining/improving (Học sinh id: 1L tăng từ 4 lên 7, HS: 2L tăng từ 6 lên 9)
        StudentSessionExam sse1_ex1 = StudentSessionExam.builder().studentId(1L).score(4.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        StudentSessionExam sse2_ex1 = StudentSessionExam.builder().studentId(2L).score(6.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(Arrays.asList(sse1_ex1, sse2_ex1));

        StudentSessionExam sse1_ex2 = StudentSessionExam.builder().studentId(1L).score(7.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        StudentSessionExam sse2_ex2 = StudentSessionExam.builder().studentId(2L).score(9.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        when(studentSessionExamRepository.findAllBySessionExamId(2L)).thenReturn(Arrays.asList(sse1_ex2, sse2_ex2));

        var result = classroomStatisticsService.getImprovementTrend(100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);
        
        assertThat(result.getTrendData()).hasSize(2);
        assertThat(result.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.IMPROVING);
        // Vì có 2 nhóm, điểm tăng đồng đều cả lớp.
        assertThat(result.getStatistics().getConsistentImprovers()).isEqualTo(2);
        assertThat(result.getStatistics().getDecliningStudents()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("TC_QLLH_STAT_07: getImprovementTrend - xu hướng DECLINING_group by WEEK")
    void getImprovementTrend_Success_Declining_GroupByWeek() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));
        // Đặt Exam nằm ở 2 tuần khác nhau để có 2 điểm trend (sessionExam1 là 2 tuần trước, sessionExam2 là 5 ngày trước)
        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                .thenReturn(Arrays.asList(sessionExam1, sessionExam2));
        
        // Cố tình tạo Mock trả về cho các hàm phụ đếm lượng tham gia
        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(anyLong())).thenReturn(1L);
        when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(anyLong(), eq(ExamSubmissionStatus.SUBMITTED))).thenReturn(1L);
        
        // Exam 1 Avg 9.0
        when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(Arrays.asList(9.0)); 
        // Exam 2 Avg 4.0
        when(studentSessionExamRepository.findAllScoresBySessionExamId(2L)).thenReturn(Arrays.asList(4.0));
        
        StudentSessionExam sse1_ex1 = StudentSessionExam.builder().studentId(1L).score(9.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(Arrays.asList(sse1_ex1));
        StudentSessionExam sse1_ex2 = StudentSessionExam.builder().studentId(1L).score(4.0).submissionStatus(ExamSubmissionStatus.SUBMITTED).build();
        when(studentSessionExamRepository.findAllBySessionExamId(2L)).thenReturn(Arrays.asList(sse1_ex2));

        var result = classroomStatisticsService.getImprovementTrend(100L, StatisticsPeriod.MONTH, StatisticsGroupBy.WEEK);
        
        assertThat(result.getTrendData()).hasSize(2);
        assertThat(result.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.DECLINING);
        // Do có 1 sinh viên duy nhất giảm từ 9 xuống 4, nên count = 1
        assertThat(result.getStatistics().getDecliningStudents()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC_QLLH_STAT_08: getImprovementTrend - gom nhóm MONTH với mốc thời gian QUARTER")
    void getImprovementTrend_GroupByMonth() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));
        // Tạo một old Exam cách đây khoảng 100 ngày (hơn 3 tháng) để test bộ lọc Period.QUARTER (chỉ tính dưới 90 ngày)
        SessionExam oldExam = SessionExam.builder().sessionExamId(3L).classId(100L).startDate(LocalDateTime.now().minusDays(100)).endDate(LocalDateTime.now().minusDays(98)).build();
        
        when(sessionExamRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                .thenReturn(Arrays.asList(sessionExam1, sessionExam2, oldExam));

        when(studentSessionExamRepository.findAllScoresBySessionExamId(anyLong())).thenReturn(Arrays.asList(5.0));
        when(studentSessionExamRepository.countTotalStudentsBySessionExamId(anyLong())).thenReturn(1L);
        when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(anyLong(), eq(ExamSubmissionStatus.SUBMITTED))).thenReturn(1L);

        // Lọc theo QUARTER (3 tháng), oldExam sẽ bị loại vì nó là 100 ngày trước (hơn 3 tháng).
        var result = classroomStatisticsService.getImprovementTrend(100L, StatisticsPeriod.QUARTER, StatisticsGroupBy.MONTH);
        
        assertThat(result).isNotNull();
        // Sẽ không bao giờ vượt qua 2 do oldExam bị loại
        assertThat(result.getTrendData().size()).isLessThanOrEqualTo(2);
    }
}
