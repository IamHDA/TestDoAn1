package com.vn.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import com.vn.backend.dto.redis.StudentInfoDTO;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.PaginationRequest;
import com.vn.backend.dto.request.common.PagingMetaRequest;
import com.vn.backend.dto.request.exam.ExamSaveAnswersRequest;
import com.vn.backend.dto.request.exam.ExamSubmissionRequest;
import com.vn.backend.dto.request.sessionexam.SaveAnswersRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamCreateRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchTeacherRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamUpdateRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsResponse;
import com.vn.backend.dto.response.exam.ExamSaveAnswersResponse;
import com.vn.backend.dto.response.exam.ExamSubmissionResponse;
import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.dto.response.sessionexam.JoinExamResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamMonitoringResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamSearchTeacherResponse;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.ExamQuestionSnapshotService;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.services.impl.SessionExamServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionExamServiceImpl Unit Tests")
class SessionExamServiceImplTest {

    @Mock
    private SessionExamRepository sessionExamRepository;
    @Mock
    private AuthService authService;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionExamMonitoringLogRepository sessionExamMonitoringLogRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ExamQuestionSnapshotService examQuestionSnapshotService;
    @Mock
    private StudentSessionExamRepository studentSessionExamRepository;
    @Mock
    private ExamQuestionSnapshotRepository examQuestionSnapshotRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private SessionExamServiceImpl sessionExamService;

    private User teacher;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(1L)
                .fullName("Teacher Name")
                .build();
        
        classroom = Classroom.builder()
                .classroomId(1L)
                .className("Class Name")
                .build();
    }

    @Test
    @DisplayName("create - thành công khi thông tin hợp lệ")
    void create_Success() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = start.plusMinutes(60);
        SessionExamCreateRequest request = SessionExamCreateRequest.builder()
                .classId(1L)
                .examId(1L)
                .title("Midterm Exam")
                .startDate(start)
                .endDate(end)
                .duration(45L)
                .examMode(ExamMode.STRICT)
                .questionOrderMode(QuestionOrderMode.SEQUENTIAL)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(true);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(1L, 1L)).thenReturn(Optional.of(classroom));

        SessionExam savedEntity = SessionExam.builder()
                .sessionExamId(100L)
                .classId(1L)
                .examId(1L)
                .title("Midterm Exam")
                .startDate(start)
                .endDate(end)
                .duration(45L)
                .build();

        when(sessionExamRepository.saveAndFlush(any(SessionExam.class))).thenReturn(savedEntity);

        SessionExamResponse response = sessionExamService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionExamId()).isEqualTo(100L);
        verify(sessionExamRepository).saveAndFlush(any(SessionExam.class));
        verify(examQuestionSnapshotService).createExamQuestionSnapshots(eq(1L), eq(100L));
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("update - thành công khi cập nhật thông tin hợp lệ")
    void update_Success() {
        LocalDateTime now = LocalDateTime.now();
        SessionExam existing = SessionExam.builder()
                .sessionExamId(1L)
                .createdBy(1L)
                .classId(1L)
                .startDate(now.plusMinutes(10))
                .endDate(now.plusMinutes(70))
                .duration(60L)
                .isDeleted(false)
                .build();

        SessionExamUpdateRequest request = SessionExamUpdateRequest.builder()
                .title("Updated Title")
                .duration(45L)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(existing));
        when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(true);
        when(sessionExamRepository.saveAndFlush(any(SessionExam.class))).thenAnswer(i -> i.getArguments()[0]);

        SessionExamResponse response = sessionExamService.update(1L, request);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(existing.getDuration()).isEqualTo(45L);
        verify(sessionExamRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("getDetail - thành công khi session exam tồn tại")
    void getDetail_Success() {
        SessionExam existing = SessionExam.builder()
                .sessionExamId(1L)
                .createdBy(1L)
                .examId(2L)
                .build();

        Exam exam = Exam.builder()
                .examId(2L)
                .title("Exam Title")
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(existing));
        when(examRepository.findByExamIdAndIsDeletedIsFalse(2L)).thenReturn(Optional.of(exam));

        var response = sessionExamService.getDetail(1L);

        assertThat(response.getSessionExamId()).isEqualTo(1L);
        assertThat(response.getExamTitle()).isEqualTo("Exam Title");
    }

    @Test
    @DisplayName("endLiveSessionExam - thành công thực hiện chấm điểm hàng loạt và cập nhật trạng thái")
    void endLiveSessionExam_Success() {
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(1L)
                .status(SessionExamStatus.ONGOING)
                .build();
        
        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .duration(60L)
                .countdownStartAt(LocalDateTime.now().minusMinutes(70)) // Ended
                .build();

        when(redisService.getExamState(anyString())).thenReturn(state);
        when(redisService.sMembers(anyString())).thenReturn(Collections.emptySet());
        when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(Collections.emptyList());

        sessionExamService.endLiveSessionExam(sessionExam);

        assertThat(sessionExam.getStatus()).isEqualTo(SessionExamStatus.ENDED);
        verify(sessionExamRepository).save(sessionExam);
        verify(sessionExamMonitoringLogRepository).save(any());
    }

    @Test
    @DisplayName("searchSessionExamByTeacher - thành công trả về danh sách ca thi của giáo viên")
    void searchSessionExamByTeacher_Success() {
        BaseFilterSearchRequest<SessionExamSearchTeacherRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SessionExamSearchTeacherRequest());
        
        PaginationRequest pagination = new PaginationRequest();
        PagingMetaRequest pagingMeta = new PagingMetaRequest();
        pagingMeta.setPageNum(1);
        pagingMeta.setPageSize(10);
        pagination.setPagingMeta(pagingMeta);
        request.setPagination(pagination);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.searchByTeacher(any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ResponseListData<SessionExamSearchTeacherResponse> response = sessionExamService.searchSessionExamByTeacher(request);

        assertThat(response.getData()).isEmpty();
        verify(sessionExamRepository).searchByTeacher(any(), any());
    }

    @Test
    @DisplayName("delete - thành công đánh dấu isDeleted = true")
    void delete_Success() {
        SessionExam existing = SessionExam.builder()
                .sessionExamId(1L)
                .createdBy(1L)
                .isDeleted(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(existing));

        sessionExamService.delete(1L);

        assertThat(existing.getIsDeleted()).isTrue();
        verify(sessionExamRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("getExamQuestions - thành công khi sinh viên trong lớp và đề thi hợp lệ")
    void getExamQuestions_Success() {
        Long sessionExamId = 1L;
        User student = User.builder().id(2L).fullName("Student Name").build();
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(sessionExamId)
                .classId(10L)
                .duration(60L)
                .questionOrderMode(QuestionOrderMode.SEQUENTIAL)
                .build();
        
        StudentSessionExam sse = StudentSessionExam.builder()
                .sessionExamId(sessionExamId)
                .studentId(2L)
                .submissionStatus(ExamSubmissionStatus.NOT_STARTED)
                .build();

        when(authService.getCurrentUser()).thenReturn(student);
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                10L, 2L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(new ClassMember()));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                .thenReturn(Optional.of(sse));
        
        when(redisService.get(anyString(), eq(ExamQuestionsResponse.class))).thenReturn(null);
        when(examQuestionSnapshotService.getAllQuestions(eq(sessionExamId), eq(QuestionOrderMode.SEQUENTIAL)))
                .thenReturn(new ArrayList<>());

        var response = sessionExamService.getExamQuestions(sessionExamId);

        assertThat(response).isNotNull();
        assertThat(sse.getExamStartTime()).isNotNull();
        verify(studentSessionExamRepository).saveAndFlush(sse);
        verify(redisService).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("saveExamAnswers - thành công khi lưu câu trả lời cho bài thi FLEXIBLE")
    void saveExamAnswers_Success() {
        Long sessionExamId = 1L;
        User student = User.builder().id(2L).build();
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(sessionExamId)
                .examMode(ExamMode.FLEXIBLE)
                .duration(60L)
                .build();
        StudentSessionExam sse = StudentSessionExam.builder()
                .examStartTime(LocalDateTime.now().minusMinutes(10))
                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED)
                .build();
        
        ExamSaveAnswersRequest request = ExamSaveAnswersRequest.builder()
                .sessionExamId(sessionExamId)
                .answers(new ArrayList<>())
                .build();

        when(authService.getCurrentUser()).thenReturn(student);
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                .thenReturn(Optional.of(sse));
        
        ExamQuestionsResponse cachedExam = ExamQuestionsResponse.builder()
                .questions(new ArrayList<>())
                .build();
        when(redisService.get(anyString(), eq(ExamQuestionsResponse.class))).thenReturn(cachedExam);

        ExamSaveAnswersResponse response = sessionExamService.saveExamAnswers(request);

        assertThat(response.getMessage()).isEqualTo("Lưu kết quả thành công");
        verify(redisService).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("submitExam - thành công tính đúng điểm và xóa dữ liệu Redis")
    void submitExam_Success() {
        Long sessionExamId = 1L;
        User student = User.builder().id(2L).build();
        SessionExam sessionExam = SessionExam.builder().sessionExamId(1L).build();
        
        StudentSessionExam sse = StudentSessionExam.builder()
                .studentSessionExamId(10L)
                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED)
                .build();

        ExamQuestionSnapshot q1 = new ExamQuestionSnapshot();
        q1.setId(100L);
        q1.setQuestionType(QuestionType.MULTI_CHOICE);
        ExamQuestionAnswerSnapshot a1 = new ExamQuestionAnswerSnapshot();
        a1.setId(1001L);
        a1.setIsCorrect(true);
        q1.setExamQuestionAnswers(List.of(a1));

        List<ExamSubmissionRequest.AnswerSubmission> answers = List.of(
                new ExamSubmissionRequest.AnswerSubmission(100L, List.of(1001L))
        );
        ExamSubmissionRequest request = ExamSubmissionRequest.builder()
                .sessionExamId(sessionExamId)
                .answers(answers)
                .build();

        when(authService.getCurrentUser()).thenReturn(student);
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                .thenReturn(Optional.of(sse));
        when(examQuestionSnapshotRepository.findAllBySessionExamId(sessionExamId)).thenReturn(List.of(q1));

        ExamSubmissionResponse response = sessionExamService.submitExam(request);

        assertThat(response.getScore()).isEqualTo(10.0);
        assertThat(sse.getSubmissionStatus()).isEqualTo(ExamSubmissionStatus.SUBMITTED);
        verify(studentSessionExamRepository).saveAndFlush(sse);
        verify(redisService).delete(anyString()); // Cleanup
    }

    @Test
    @DisplayName("joinSessionExam - thành công khi sinh viên đủ điều kiện")
    void joinSessionExam_Success() {
        Long sessionExamId = 1L;
        User student = User.builder().id(2L).email("student@test.com").build();
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(sessionExamId)
                .title("Strict Exam")
                .duration(60L)
                .build();
        
        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .countdownStartAt(LocalDateTime.now().minusMinutes(1))
                .duration(60L)
                .build();

        when(authService.getCurrentUser()).thenReturn(student);
        when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(sessionExamId))
                .thenReturn(Optional.of(sessionExam));
        when(redisService.sIsMember(anyString(), eq("2"))).thenReturn(true);
        when(redisService.getExamState(anyString())).thenReturn(state);
        when(redisService.parseString(any())).thenReturn("token123");

        JoinExamResponse response = sessionExamService.joinSessionExam(sessionExamId);

        assertThat(response.getSessionExamId()).isEqualTo(sessionExamId);
        assertThat(response.getPhase()).isEqualTo(SessionExamPhase.JOINING);
        verify(webSocketService).broadcastStudentJoined(eq(sessionExamId), eq(student), any());
    }

    @Test
    @DisplayName("saveAnswers (Strict) - thành công khi token hợp lệ và đang trong giờ thi")
    void saveAnswers_Strict_Success() {
        Long sessionExamId = 1L;
        String token = "valid-token";
        SaveAnswersRequest request = new SaveAnswersRequest();
        request.setAnswers(new ArrayList<>());
        
        StudentInfoDTO studentInfo = new StudentInfoDTO();
        studentInfo.setStudentId(2L);
        
        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .countdownStartAt(LocalDateTime.now().minusMinutes(10))
                .duration(60L)
                .build();

        when(redisService.get(anyString(), eq(StudentInfoDTO.class))).thenReturn(studentInfo);
        when(redisService.getExamState(anyString())).thenReturn(state);
        when(redisService.hLen(anyString())).thenReturn(0L);

        var response = sessionExamService.saveAnswers(sessionExamId, token, request);

        assertThat(response.getTotalAnswered()).isEqualTo(0);
        verify(redisService).hSetAll(anyString(), anyMap());
    }

    @Test
    @DisplayName("getExamMonitoring - thành công trả về dữ liệu giám sát cho giáo viên")
    void getExamMonitoring_Success() {
        Long sessionExamId = 1L;
        User teacher = User.builder().id(1L).build();
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(sessionExamId)
                .createdBy(1L)
                .status(SessionExamStatus.ONGOING)
                .build();
        
        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .title("Monitoring Test")
                .countdownStartAt(LocalDateTime.now().minusMinutes(10))
                .duration(60L)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(sessionExamId, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(redisService.getExamState(anyString())).thenReturn(state);
        when(redisService.sMembers(anyString())).thenReturn(new java.util.HashSet<>());

        SessionExamMonitoringResponse response = sessionExamService.getExamMonitoring(sessionExamId);

        assertThat(response.getTitle()).isEqualTo("Monitoring Test");
        assertThat(response.getPhase()).isEqualTo(SessionExamPhase.ONGOING);
    }

    @Test
    @DisplayName("saveExamAnswers - ném exception khi gọi cho bài thi STRICT (chỉ dùng cho FLEXIBLE)")
    void saveExamAnswers_ThrowsForbidden_WhenStrictMode() {
        Long sessionExamId = 1L;
        SessionExam sessionExam = SessionExam.builder()
                .sessionExamId(sessionExamId)
                .examMode(ExamMode.STRICT)
                .build();
        
        ExamSaveAnswersRequest request = ExamSaveAnswersRequest.builder().sessionExamId(sessionExamId).build();

        when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));

        assertThatThrownBy(() -> sessionExamService.saveExamAnswers(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Chỉ có thể lưu kết quả cho bài thi FLEXIBLE");
    }

    @Test
    @DisplayName("submitExam - ném exception khi sinh viên đã nộp bài trước đó")
    void submitExam_ThrowsForbidden_WhenAlreadySubmitted() {
        Long sessionExamId = 1L;
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionStatus(ExamSubmissionStatus.SUBMITTED)
                .build();

        when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(new SessionExam()));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                .thenReturn(Optional.of(sse));

        ExamSubmissionRequest request = ExamSubmissionRequest.builder().sessionExamId(sessionExamId).build();

        assertThatThrownBy(() -> sessionExamService.submitExam(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Bạn đã nộp bài thi này rồi");
    }

    @Test
    @DisplayName("getExamQuestions - ném exception khi sinh viên không thuộc lớp học")
    void getExamQuestions_ThrowsForbidden_WhenStudentNotInClass() {
        Long sessionExamId = 1L;
        SessionExam sessionExam = SessionExam.builder().sessionExamId(sessionExamId).classId(10L).build();

        when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
        when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionExamService.getExamQuestions(sessionExamId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("create - ném exception khi không có quyền (không phải giáo viên lớp)")
    void create_ThrowsForbidden_WhenNotTeacherOfClass() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = start.plusMinutes(60);
        SessionExamCreateRequest request = SessionExamCreateRequest.builder()
                .classId(1L)
                .startDate(start)
                .endDate(end)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(false);
        when(messageUtils.getMessage(any())).thenReturn("Forbidden");

        assertThatThrownBy(() -> sessionExamService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("create - ném exception khi thời gian không hợp lệ (bắt đầu quá sớm)")
    void create_ThrowsException_WhenStartTimeTooEarly() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(2); // Less than 5 mins
        LocalDateTime end = start.plusMinutes(60);
        SessionExamCreateRequest request = SessionExamCreateRequest.builder()
                .startDate(start)
                .endDate(end)
                .build();

        assertThatThrownBy(() -> sessionExamService.create(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Start date must be at least 5 minutes from now");
    }
}
