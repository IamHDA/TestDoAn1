package com.vn.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.backend.dto.redis.SessionExamContentDTO;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import com.vn.backend.dto.redis.StudentInfoDTO;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.exam.ExamSaveAnswersRequest;
import com.vn.backend.dto.request.exam.ExamSubmissionRequest;
import com.vn.backend.dto.request.sessionexam.*;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsResponse;
import com.vn.backend.dto.response.exam.ExamSaveAnswersResponse;
import com.vn.backend.dto.response.exam.ExamSubmissionResponse;
import com.vn.backend.dto.response.sessionexam.*;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.ExamQuestionSnapshotService;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.services.impl.SessionExamServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionExamServiceImpl Unit Tests")
@TestMethodOrder(MethodOrderer.DisplayName.class)
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
        private User student;
        private Classroom classroom;
        private SessionExam sessionExam;

        @BeforeEach
        void setUp() {
                teacher = User.builder()
                                .id(1L)
                                .fullName("Teacher Name")
                                .build();

                student = User.builder()
                                .id(2L)
                                .fullName("Student Name")
                                .role(Role.STUDENT)
                                .build();

                classroom = Classroom.builder()
                                .classroomId(1L)
                                .className("Class Name")
                                .build();

                sessionExam = SessionExam.builder()
                                .sessionExamId(1L)
                                .classId(1L)
                                .examId(1L)
                                .createdBy(1L)
                                .status(SessionExamStatus.NOT_STARTED)
                                .startDate(LocalDateTime.now().plusMinutes(10))
                                .endDate(LocalDateTime.now().plusHours(2))
                                .isDeleted(false)
                                .build();
        }

        @Test
        @DisplayName("[TC_SE_01] create - thành công khi thông tin hợp lệ")
        void create_Success() {
                LocalDateTime start = LocalDateTime.now().plusMinutes(10);
                LocalDateTime end = start.plusMinutes(60);
                SessionExamCreateRequest request = new SessionExamCreateRequest();
                request.setClassId(1L);
                request.setExamId(1L);
                request.setTitle("Midterm Exam");
                request.setStartDate(start);
                request.setEndDate(end);
                request.setDuration(45L);
                request.setExamMode(ExamMode.LIVE);
                request.setQuestionOrderMode(QuestionOrderMode.SEQUENTIAL);

                when(authService.getCurrentUser()).thenReturn(teacher);
                when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(true);
                when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(1L, 1L))
                                .thenReturn(Optional.of(classroom));

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
        @DisplayName("[TC_SE_02] update - thành công khi cập nhật thông tin hợp lệ")
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

                SessionExamUpdateRequest request = new SessionExamUpdateRequest();
                request.setTitle("Updated Title");
                request.setDuration(45L);

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
        @DisplayName("[TC_SE_03] getDetail - thành công khi session exam tồn tại")
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
        @DisplayName("[TC_SE_04] endLiveSessionExam - thành công thực hiện chấm điểm hàng loạt và cập nhật trạng thái")
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
        @DisplayName("[TC_SE_05] searchSessionExamByTeacher - thành công trả về danh sách ca thi của giáo viên")
        void searchSessionExamByTeacher_Success() {
                BaseFilterSearchRequest<SessionExamSearchTeacherRequest> request = new BaseFilterSearchRequest<>();
                request.setFilters(new SessionExamSearchTeacherRequest());

                SearchRequest pagination = new SearchRequest();
                pagination.setPageNum("1");
                pagination.setPageSize("10");
                request.setPagination(pagination);

                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.searchByTeacher(any(), any()))
                                .thenReturn(new PageImpl<>(Collections.emptyList()));

                ResponseListData<SessionExamSearchTeacherResponse> response = sessionExamService
                                .searchSessionExamByTeacher(request);

                assertThat(response.getContent()).isEmpty();
                verify(sessionExamRepository).searchByTeacher(any(), any());
        }

        @Test
        @DisplayName("[TC_SE_06] delete - thành công đánh dấu isDeleted = true")
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
        @DisplayName("[TC_SE_07] getExamQuestions - thành công khi sinh viên trong lớp và đề thi hợp lệ")
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
                                .thenReturn(Optional.of(new com.vn.backend.entities.ClassMember()));
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
        @DisplayName("[TC_SE_08] saveExamAnswers - thành công khi lưu câu trả lời cho bài thi FLEXIBLE")
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

                ExamSaveAnswersRequest request = new ExamSaveAnswersRequest();
                request.setSessionExamId(sessionExamId);
                request.setAnswers(new ArrayList<>());

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
        @DisplayName("[TC_SE_09] submitExam - thành công tính đúng điểm và xóa dữ liệu Redis")
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

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);

                ExamSubmissionRequest.AnswerSubmission answerSubmission = new ExamSubmissionRequest.AnswerSubmission();
                answerSubmission.setQuestionSnapshotId(100L);
                answerSubmission.setSelectedAnswerIds(List.of(1001L));
                request.setAnswers(List.of(answerSubmission));

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
        @DisplayName("[TC_SE_10] joinSessionExam - thành công khi sinh viên đủ điều kiện")
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
                assertThat(response.getPhase()).isEqualTo(SessionExamPhase.COUNTDOWN);
                verify(webSocketService).broadcastStudentJoined(eq(sessionExamId), eq(student), any());
        }

        @Test
        @DisplayName("[TC_SE_11] saveAnswers (Strict) - thành công khi token hợp lệ và đang trong giờ thi")
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
        @DisplayName("[TC_SE_12] getExamMonitoring - thành công trả về dữ liệu giám sát cho giáo viên")
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
        @DisplayName("[TC_SE_13] saveExamAnswers - ném exception khi gọi cho bài thi STRICT (chỉ dùng cho FLEXIBLE)")
        void saveExamAnswers_ThrowsForbidden_WhenStrictMode() {
                Long sessionExamId = 1L;
                SessionExam sessionExam = SessionExam.builder()
                                .sessionExamId(sessionExamId)
                                .examMode(ExamMode.LIVE)
                                .build();

                ExamSaveAnswersRequest request = new ExamSaveAnswersRequest();
                request.setSessionExamId(sessionExamId);

                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));

                assertThatThrownBy(() -> sessionExamService.saveExamAnswers(request))
                                .isInstanceOf(AppException.class)
                                .hasMessageContaining("Chỉ có thể lưu kết quả cho bài thi FLEXIBLE");
        }

        @Test
        @DisplayName("[TC_SE_14] submitExam - ném exception khi sinh viên đã nộp bài trước đó")
        void submitExam_ThrowsForbidden_WhenAlreadySubmitted() {
                Long sessionExamId = 1L;
                StudentSessionExam sse = StudentSessionExam.builder()
                                .submissionStatus(ExamSubmissionStatus.SUBMITTED)
                                .build();

                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(new SessionExam()));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                                .thenReturn(Optional.of(sse));

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);

                assertThatThrownBy(() -> sessionExamService.submitExam(request))
                                .isInstanceOf(AppException.class)
                                .hasMessageContaining("Bạn đã nộp bài thi này rồi");
        }

        @Test
        @DisplayName("[TC_SE_15] getExamQuestions - ném exception khi sinh viên không thuộc lớp học")
        void getExamQuestions_ThrowsForbidden_WhenStudentNotInClass() {
                Long sessionExamId = 1L;
                SessionExam sessionExam = SessionExam.builder().sessionExamId(sessionExamId).classId(10L).build();

                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
                when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(),
                                any(), any()))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> sessionExamService.getExamQuestions(sessionExamId))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("[TC_SE_16] create - ném exception khi không có quyền (không phải giáo viên lớp)")
        void create_ThrowsForbidden_WhenNotTeacherOfClass() {
                LocalDateTime start = LocalDateTime.now().plusMinutes(10);
                LocalDateTime end = start.plusMinutes(60);
                SessionExamCreateRequest request = new SessionExamCreateRequest();
                request.setClassId(1L);
                request.setStartDate(start);
                request.setEndDate(end);

                when(authService.getCurrentUser()).thenReturn(teacher);
                when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(false);
                when(messageUtils.getMessage(any())).thenReturn("Forbidden");

                assertThatThrownBy(() -> sessionExamService.create(request))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("[TC_SE_17] create - ném exception khi thời gian không hợp lệ (bắt đầu quá sớm)")
        void create_ThrowsException_WhenStartTimeTooEarly() {
                LocalDateTime start = LocalDateTime.now().plusMinutes(2); // Less than 5 mins
                LocalDateTime end = start.plusMinutes(60);
                SessionExamCreateRequest request = new SessionExamCreateRequest();
                request.setStartDate(start);
                request.setEndDate(end);

                assertThatThrownBy(() -> sessionExamService.create(request))
                                .isInstanceOf(AppException.class)
                                .hasMessageContaining("Start date must be at least 5 minutes from now");
        }

        @Test
        @DisplayName("[TC_SE_18] create - ném exception khi thời gian kết thúc trước thời gian bắt đầu")
        void create_ThrowsException_WhenEndTimeBeforeStartTime() {
                LocalDateTime start = LocalDateTime.now().plusMinutes(10);
                LocalDateTime end = start.minusMinutes(5); // Lỗi logic: End < Start
                SessionExamCreateRequest request = new SessionExamCreateRequest();
                request.setStartDate(start);
                request.setEndDate(end);

                assertThatThrownBy(() -> sessionExamService.create(request))
                                .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_19] update - ném exception khi cố tình sửa ca thi đang diễn ra (ONGOING) chống gian lận")
        void update_ThrowsException_WhenExamOngoing() {
                SessionExam existing = SessionExam.builder()
                                .sessionExamId(1L)
                                .createdBy(1L)
                                .status(SessionExamStatus.ONGOING) // Đang diễn ra
                                .isDeleted(false)
                                .build();

                SessionExamUpdateRequest request = new SessionExamUpdateRequest();
                request.setTitle("Hacked Title");

                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.of(existing));

                // Expect to throw AppException because we can't update ONGOING exam
                assertThatThrownBy(() -> sessionExamService.update(1L, request))
                                .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_20] joinSessionExam - trả về NOT_STARTED khi sinh viên vào quá sớm thay vì cho phép thi")
        void joinSessionExam_ReturnsNotStarted_WhenExamTooEarly() {
                Long sessionExamId = 1L;
                User student = User.builder().id(2L).build();
                SessionExam sessionExam = SessionExam.builder().sessionExamId(sessionExamId).build();

                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(null) // Chưa ấn định đếm ngược -> NOT_STARTED
                                .duration(60L)
                                .build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(sessionExamId))
                                .thenReturn(Optional.of(sessionExam));
                when(redisService.sIsMember(anyString(), eq("2"))).thenReturn(true);
                when(redisService.getExamState(anyString())).thenReturn(state);

                JoinExamResponse response = sessionExamService.joinSessionExam(sessionExamId);

                // Hệ thống sẽ trả về response có phase NOT_STARTED chứ không tung Exception
                assertThat(response.getPhase()).isEqualTo(SessionExamPhase.NOT_STARTED);
        }

        @Test
        @DisplayName("[TC_SE_21] submitExam - thành công (0 điểm) kể cả khi nộp giấy trắng (Mảng answers rỗng)")
        void submitExam_Success_WhenSubmittingBlankPaper() {
                Long sessionExamId = 1L;
                User student = User.builder().id(2L).build();
                SessionExam sessionExam = SessionExam.builder().sessionExamId(1L).build();

                StudentSessionExam sse = StudentSessionExam.builder()
                                .studentSessionExamId(10L)
                                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED)
                                .build();

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);
                request.setAnswers(new ArrayList<>()); // Cố tình nộp mảng rỗng (Giấy trắng)

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(sessionExam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                                .thenReturn(Optional.of(sse));

                // Mock DB không trả về câu hỏi nào hoặc code tự đối chiếu đáp án rỗng ra 0
                when(examQuestionSnapshotRepository.findAllBySessionExamId(sessionExamId))
                                .thenReturn(new ArrayList<>());

                ExamSubmissionResponse response = sessionExamService.submitExam(request);

                assertThat(response.getScore()).isEqualTo(0.0); // Chắc chắn là 0 điểm
                assertThat(sse.getSubmissionStatus()).isEqualTo(ExamSubmissionStatus.SUBMITTED);
        }

        @Test
        @DisplayName("[TC_SE_22] update - ném exception khi ca thi đã ENDED")
        void update_ThrowsException_WhenExamEnded() {
                SessionExam existing = SessionExam.builder().sessionExamId(1L).status(SessionExamStatus.ENDED).build();
                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.of(existing));
                assertThatThrownBy(() -> sessionExamService.update(1L, new SessionExamUpdateRequest()))
                                .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_23] delete - ném exception khi ca thi đang ONGOING")
        void delete_ThrowsException_WhenExamOngoing() {
                SessionExam existing = SessionExam.builder().sessionExamId(1L).status(SessionExamStatus.ONGOING)
                                .build();
                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.of(existing));
                assertThatThrownBy(() -> sessionExamService.delete(1L)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_24] delete - ném exception khi ca thi đã ENDED")
        void delete_ThrowsException_WhenExamEnded() {
                SessionExam existing = SessionExam.builder().sessionExamId(1L).status(SessionExamStatus.ENDED).build();
                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.of(existing));
                assertThatThrownBy(() -> sessionExamService.delete(1L)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_25] getDetail - ném exception khi ca thi không tồn tại")
        void getDetail_ThrowsException_WhenExamNotFound() {
                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.empty());
                assertThatThrownBy(() -> sessionExamService.getDetail(1L)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_26] create - ném exception khi không tìm thấy ExamId gốc")
        void create_ThrowsException_WhenExamIdNotFound() {
                SessionExamCreateRequest request = new SessionExamCreateRequest();
                request.setClassId(1L);
                request.setExamId(99L);
                request.setStartDate(LocalDateTime.now().plusMinutes(10));
                request.setEndDate(LocalDateTime.now().plusMinutes(60));
                when(authService.getCurrentUser()).thenReturn(teacher);
                when(classroomRepository.existsByClassroomIdAndTeacherId(1L, 1L)).thenReturn(true);
                when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(1L, 1L))
                                .thenReturn(Optional.of(classroom));
                when(sessionExamRepository.saveAndFlush(any(SessionExam.class)))
                                .thenThrow(new AppException(MessageConst.NOT_FOUND, "Not found", HttpStatus.NOT_FOUND));

                assertThatThrownBy(() -> sessionExamService.create(request)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_27] endLiveSessionExam - không nổ lỗi khi phòng thi không có ai")
        void endLiveSessionExam_DoesNotFail_WhenNoStudents() {
                SessionExam sessionExam = SessionExam.builder().sessionExamId(1L).status(SessionExamStatus.ONGOING)
                                .build();
                SessionExamStateDTO state = SessionExamStateDTO.builder().duration(60L)
                                .countdownStartAt(LocalDateTime.now().minusMinutes(70)).build();
                when(redisService.getExamState(anyString())).thenReturn(state);
                when(redisService.sMembers(anyString())).thenReturn(Collections.emptySet()); // Không có học sinh
                when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(Collections.emptyList());

                sessionExamService.endLiveSessionExam(sessionExam);
                assertThat(sessionExam.getStatus()).isEqualTo(SessionExamStatus.ENDED);
        }

        @Test
        @DisplayName("[TC_SE_28] joinSessionExam - ném exception khi ca thi đã ENDED")
        void joinSessionExam_ThrowsException_WhenExamEnded() {
                Long id = 1L;
                User student = User.builder().id(2L).build();
                SessionExam sessionExam = SessionExam.builder().sessionExamId(id).build();
                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(LocalDateTime.now().minusMinutes(70)).duration(60L).build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(id))
                                .thenReturn(Optional.of(sessionExam));
                when(redisService.getExamState(anyString())).thenReturn(state);

                assertThatThrownBy(() -> sessionExamService.joinSessionExam(id)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_29] joinSessionExam - ném exception khi sinh viên không có quyền (Not Eligible)")
        void joinSessionExam_ThrowsException_WhenNotEligible() {
                Long id = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(id)).thenReturn(Optional.of(exam));
                when(redisService.sIsMember(anyString(), eq("2"))).thenReturn(false);

                assertThatThrownBy(() -> sessionExamService.joinSessionExam(id)).isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("[TC_SE_30] getExamQuestions - ném exception khi không tìm thấy ca thi")
        void getExamQuestions_ThrowsException_WhenExamNotFound() {
                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.empty());
                assertThatThrownBy(() -> sessionExamService.getExamQuestions(1L)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_31] getExamQuestions - truy vấn Database khi Redis Cache trống")
        void getExamQuestions_FallbackToDatabase_WhenRedisCacheIsEmpty() {
                Long id = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).classId(1L).duration(60L)
                                .questionOrderMode(QuestionOrderMode.SEQUENTIAL).build();
                StudentSessionExam sse = StudentSessionExam.builder().submissionStatus(ExamSubmissionStatus.NOT_STARTED)
                                .build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(id)).thenReturn(Optional.of(exam));
                when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(),
                                any(), any())).thenReturn(Optional.of(new com.vn.backend.entities.ClassMember()));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(id, 2L))
                                .thenReturn(Optional.of(sse));

                when(redisService.get(anyString(), eq(ExamQuestionsResponse.class))).thenReturn(null); // Cache trống
                when(examQuestionSnapshotService.getAllQuestions(anyLong(), any())).thenReturn(new ArrayList<>()); // DB
                                                                                                                   // chạy

                ExamQuestionsResponse res = sessionExamService.getExamQuestions(id);
                assertThat(res).isNotNull();
                verify(examQuestionSnapshotService).getAllQuestions(anyLong(), any()); // Xác nhận có xuống DB
        }

        @Test
        @DisplayName("[TC_SE_32] saveExamAnswers - ném exception khi ca thi không tồn tại")
        void saveExamAnswers_ThrowsException_WhenExamNotFound() {
                ExamSaveAnswersRequest request = new ExamSaveAnswersRequest();
                request.setSessionExamId(1L);
                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.empty());
                assertThatThrownBy(() -> sessionExamService.saveExamAnswers(request)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_33] saveExamAnswers - ném exception khi sinh viên đã học nộp bài (SUBMITTED)")
        void saveExamAnswers_ThrowsException_WhenAlreadySubmitted() {
                ExamSaveAnswersRequest request = new ExamSaveAnswersRequest();
                request.setSessionExamId(1L);
                SessionExam exam = SessionExam.builder().examMode(ExamMode.FLEXIBLE).build();
                StudentSessionExam sse = StudentSessionExam.builder().submissionStatus(ExamSubmissionStatus.SUBMITTED)
                                .build();

                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.of(exam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(1L, 2L))
                                .thenReturn(Optional.of(sse));

                assertThatThrownBy(() -> sessionExamService.saveExamAnswers(request)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_34] submitExam - ném exception khi ca thi không tồn tại")
        void submitExam_ThrowsException_WhenExamNotFound() {
                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(1L);
                when(authService.getCurrentUser()).thenReturn(User.builder().id(2L).build());
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.empty());
                assertThatThrownBy(() -> sessionExamService.submitExam(request)).isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_35] submitExam - tính điểm chính xác khi làm đúng 1 nửa số câu (5.0 điểm)")
        void submitExam_Success_WithPartialCorrectAnswers() {
                Long sessionExamId = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).build();
                StudentSessionExam sse = StudentSessionExam.builder().studentSessionExamId(10L)
                                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED).build();

                // Mock 2 câu hỏi, làm đúng 1 câu
                ExamQuestionSnapshot q1 = new ExamQuestionSnapshot();
                q1.setId(100L);
                q1.setQuestionType(QuestionType.MULTI_CHOICE);
                ExamQuestionAnswerSnapshot a1 = new ExamQuestionAnswerSnapshot();
                a1.setId(1001L);
                a1.setIsCorrect(true);
                q1.setExamQuestionAnswers(List.of(a1));

                ExamQuestionSnapshot q2 = new ExamQuestionSnapshot();
                q2.setId(200L);
                q2.setQuestionType(QuestionType.MULTI_CHOICE);
                ExamQuestionAnswerSnapshot a2 = new ExamQuestionAnswerSnapshot();
                a2.setId(2001L);
                a2.setIsCorrect(true);
                q2.setExamQuestionAnswers(List.of(a2));

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);

                ExamSubmissionRequest.AnswerSubmission ans1 = new ExamSubmissionRequest.AnswerSubmission();
                ans1.setQuestionSnapshotId(100L);
                ans1.setSelectedAnswerIds(List.of(1001L)); // Đúng

                ExamSubmissionRequest.AnswerSubmission ans2 = new ExamSubmissionRequest.AnswerSubmission();
                ans2.setQuestionSnapshotId(200L);
                ans2.setSelectedAnswerIds(List.of(9999L)); // Sai tòe

                request.setAnswers(List.of(ans1, ans2));

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(exam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                                .thenReturn(Optional.of(sse));
                when(examQuestionSnapshotRepository.findAllBySessionExamId(sessionExamId)).thenReturn(List.of(q1, q2));

                ExamSubmissionResponse response = sessionExamService.submitExam(request);
                assertThat(response.getScore()).isEqualTo(5.0); // (1/2) * 10
        }

        @Test
        @DisplayName("[TC_SE_36] saveAnswers_Strict - trả về 0 khi không nộp đáp án nào")
        void saveAnswers_Strict_ReturnsZero_WhenNoAnswersProvided() {
                SaveAnswersRequest req = new SaveAnswersRequest();
                req.setAnswers(new ArrayList<>());
                StudentInfoDTO info = new StudentInfoDTO();
                info.setStudentId(2L);
                SessionExamStateDTO state = SessionExamStateDTO.builder().duration(60L)
                                .countdownStartAt(LocalDateTime.now().minusMinutes(5)).build();

                when(redisService.get(anyString(), eq(StudentInfoDTO.class))).thenReturn(info);
                when(redisService.getExamState(anyString())).thenReturn(state);
                when(redisService.hLen(anyString())).thenReturn(0L); // Redis ko có dữ liệu

                var response = sessionExamService.saveAnswers(1L, "token", req);
                assertThat(response.getTotalAnswered()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC_SE_37] saveAnswers_Strict - ném exception khi Token sai/phiên hết hạn (Invalid Token)")
        void saveAnswers_Strict_ThrowsException_WhenTokenInvalid() {
                when(redisService.get(anyString(), eq(StudentInfoDTO.class))).thenReturn(null); // Sai token
                assertThatThrownBy(() -> sessionExamService.saveAnswers(1L, "faketoken", new SaveAnswersRequest()))
                                .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_38] getExamMonitoring - ném exception khi không phải Giáo viên tạo đề")
        void getExamMonitoring_ThrowsException_WhenNotTeacherOwner() {
                User teacherHack = User.builder().id(99L).build();
                when(authService.getCurrentUser()).thenReturn(teacherHack);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 99L))
                                .thenReturn(Optional.empty());
                assertThatThrownBy(() -> sessionExamService.getExamMonitoring(1L)).isInstanceOf(AppException.class);
        }

        // ===========================================
        // V. Nhóm Thống Kê & Kết Quả Điểm Thi
        // ===========================================

        @Test
        @DisplayName("[TC_SE_39] getDescriptiveStatistic_Success - Tính toán thống kê khi có kết quả")
        void getDescriptiveStatistic_Success() {
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.of(sessionExam));
                when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(List.of(5.0, 8.0, 10.0));
                when(studentSessionExamRepository.countTotalStudentsBySessionExamId(1L)).thenReturn(3L);
                when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(1L,
                                ExamSubmissionStatus.SUBMITTED)).thenReturn(3L);

                var response = sessionExamService.getDescriptiveStatistic(1L);
                assertThat(response).isNotNull();
                assertThat(response.getMean()).isNotNull();
        }

        @Test
        @DisplayName("[TC_SE_40] getDescriptiveStatistic_ZeroStudents - Bẫy trường hợp phòng thi rỗng")
        void getDescriptiveStatistic_ZeroStudents() {
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.of(sessionExam));
                when(studentSessionExamRepository.findAllScoresBySessionExamId(1L)).thenReturn(new ArrayList<>());
                when(studentSessionExamRepository.countTotalStudentsBySessionExamId(1L)).thenReturn(0L);
                when(studentSessionExamRepository.countSubmittedStudentsBySessionExamId(1L,
                                ExamSubmissionStatus.SUBMITTED)).thenReturn(0L);

                var response = sessionExamService.getDescriptiveStatistic(1L);
                assertThat(response).isNotNull();
                assertThat(response.getMean()).isNull(); // Không có data, mean = null
        }

        @Test
        @DisplayName("[TC_SE_41] getExamResults_Success - Lấy bảng điểm toàn phòng")
        void getExamResults_Success() {
                when(authService.getCurrentUser()).thenReturn(teacher);
                // getExamResults gọi findBySessionExamIdAndCreatedByAndIsDeletedFalse
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(1L, 1L))
                                .thenReturn(Optional.of(sessionExam));
                // Repository thực dùng: getExamResults(sessionExamId, classroomId, pageable)
                // trả về Page<Object[]>
                when(studentSessionExamRepository.getExamResults(eq(1L), eq(1L), any()))
                                .thenReturn(new PageImpl<>(new ArrayList<>()));

                var filterReq = new BaseFilterSearchRequest<>();
                filterReq.setPagination(createPagnination());
                var res = sessionExamService.getExamResults(1L, filterReq);
                assertThat(res).isNotNull();
        }

        // ===========================================
        // VI. Nhóm Góc Nhìn Của Sinh Viên
        // ===========================================

        @Test
        @DisplayName("[TC_SE_42] searchSessionExamByStudent_Success - Tìm kiếm ca thi cho SV")
        void searchSessionExamByStudent_Success() {
                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.searchByStudent(any(), any()))
                                .thenReturn(new PageImpl<>(new ArrayList<>()));

                var req = new BaseFilterSearchRequest<SessionExamSearchStudentRequest>();
                req.setPagination(createPagnination());
                req.setFilters(new SessionExamSearchStudentRequest());

                var res = sessionExamService.searchSessionExamByStudent(req);
                assertThat(res).isNotNull();
        }

        @Test
        @DisplayName("[TC_SE_43] getStudentExamOverview_Success - Xuất thẻ tổng quan sau thi")
        void getStudentExamOverview_Success() {
                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.of(sessionExam));
                when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                                eq(1L), eq(2L), eq(ClassMemberRole.STUDENT), eq(ClassMemberStatus.ACTIVE)))
                                .thenReturn(Optional.of(new com.vn.backend.entities.ClassMember()));
                // SV chưa bắt đầu - Optional.empty()
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(1L, 2L))
                                .thenReturn(Optional.empty());
                when(examQuestionSnapshotRepository.countBySessionExamId(1L)).thenReturn(10L);

                var res = sessionExamService.getStudentExamOverview(1L);
                assertThat(res).isNotNull();
                assertThat(res.getTotalQuestions()).isEqualTo(10);
        }

        @Test
        @DisplayName("[TC_SE_44] getStudentExamResult_Success - Chi tiết bài làm sau thi")
        void getStudentExamResult_Success() {
                when(authService.getCurrentUser()).thenReturn(student);
                when(studentSessionExamRepository.getStudentExamResult(1L, 2L)).thenReturn(null); // Không có kết quả
                when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

                assertThatThrownBy(() -> sessionExamService.getStudentExamResult(1L))
                                .isInstanceOf(AppException.class)
                                .hasMessage("Not found");
        }

        // ===========================================
        // VII. Nhóm Tự Động Hóa & Chế Độ Live
        // ===========================================

        @Test
        @DisplayName("[TC_SE_45] startLiveSessionExam_Success - Bật phòng LIVE thành công")
        void startLiveSessionExam_Success() {
                SessionExam se = SessionExam.builder()
                                .sessionExamId(1L)
                                .duration(60L)
                                .endDate(LocalDateTime.now().plusHours(2))
                                .build();
                when(redisService.exists(anyString())).thenReturn(false); // Chưa khởi động
                when(studentSessionExamRepository.findAllBySessionExamId(1L)).thenReturn(new ArrayList<>());
                when(examQuestionSnapshotRepository.findAllBySessionExamId(1L)).thenReturn(new ArrayList<>());
                doNothing().when(redisService).hSetAll(anyString(), anyMap());
                doNothing().when(redisService).delete(anyString());
                doNothing().when(redisService).expire(anyString(), anyLong(), any());

                sessionExamService.startLiveSessionExam(se);
                verify(redisService).hSetAll(anyString(), anyMap());
        }

        @Test
        @DisplayName("[TC_SE_46] startLiveSessionExam_ThrowsForbidden_WhenAlreadyStarted")
        void startLiveSessionExam_ThrowsForbidden_WhenAlreadyStarted() {
                SessionExam se = SessionExam.builder()
                                .sessionExamId(1L)
                                .endDate(LocalDateTime.now().plusHours(2))
                                .build();
                when(redisService.exists(anyString())).thenReturn(true); // Đã bắt đầu rồi
                when(messageUtils.getMessage(AppConst.MessageConst.EXAM_ALREADY_STARTED)).thenReturn("Already started");

                assertThatThrownBy(() -> sessionExamService.startLiveSessionExam(se))
                                .isInstanceOf(AppException.class)
                                .hasMessage("Already started");
        }

        @Test
        @DisplayName("[TC_SE_47] processFlexExamStarted - Quét tài liệu làm bài còn lại và auto-submit khi hết giờ")
        void processFlexExamStarted_WithNoActiveExams() {
                // findActiveFlexExams trả về rỗng => không có gì để xử lý
                when(studentSessionExamRepository.findActiveFlexExams(ExamSubmissionStatus.NOT_SUBMITTED))
                                .thenReturn(new ArrayList<>());

                // Gọi hàm - phải chạy trơn tảu không crash
                sessionExamService.processFlexExamStarted(LocalDateTime.now());

                // Không có bài nào được lưu
                verify(studentSessionExamRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[TC_SE_48] processExpiredNotStartedFlexExams - Không crash khi danh sách rỗng")
        void processExpiredNotStartedFlexExams_EmptyList() {
                // findNotStartedExpiredFlexExamsWithSessionExam trả về rỗng => return sớm
                when(studentSessionExamRepository.findNotStartedExpiredFlexExamsWithSessionExam(
                                any(), eq(ExamSubmissionStatus.NOT_STARTED)))
                                .thenReturn(new ArrayList<>());

                // Gọi hàm - phải chạy trơn tảu không crash
                sessionExamService.processExpiredNotStartedFlexExams(LocalDateTime.now());

                // Không có snapshot nào được tra cứu
                verify(examQuestionSnapshotRepository, never()).findAllBySessionExamId(any());
        }

        // ===========================================
        // VIII. Nhóm Tải Đề Thi
        // ===========================================

        @Test
        @DisplayName("[TC_SE_49] downloadExam - ném exception khi Token không hợp lệ")
        void downloadExam_ThrowsException_WhenTokenInvalid() {
                // Token không tìm thấy trong Redis => StudentInfoDTO = null => quăng lỗi
                when(redisService.get(anyString(), eq(StudentInfoDTO.class))).thenReturn(null);
                when(messageUtils.getMessage(anyString())).thenReturn("Invalid token");

                assertThatThrownBy(() -> sessionExamService.downloadExam(1L, "invalid-token"))
                                .isInstanceOf(AppException.class);
        }

        // ===========================================
        // IX. Nhóm Mở Rộng - Branch Coverage (TC_SE_50+)
        // ===========================================

        @Test
        @DisplayName("[TC_SE_50] joinSessionExam_Rejoin_Success - Sinh viên quay lại phòng thi đã join")
        void joinSessionExam_Rejoin_Success() {
                Long id = 1L;
                User student = User.builder().id(2L).email("student@test.com").build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).build();
                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(LocalDateTime.now().minusMinutes(10))
                                .duration(60L).build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(id)).thenReturn(Optional.of(exam));
                when(redisService.sIsMember(contains("eligible"), eq("2"))).thenReturn(true);
                when(redisService.getExamState(anyString())).thenReturn(state);

                when(redisService.sIsMember(contains("joined"), eq("2"))).thenReturn(true); // Already joined
                when(redisService.hGet(anyString(), eq(AppConst.FieldConst.SESSION_TOKEN)))
                                .thenReturn("existing-token");
                when(redisService.parseString("existing-token")).thenReturn("existing-token");

                JoinExamResponse response = sessionExamService.joinSessionExam(id);

                assertThat(response.getSessionToken()).isEqualTo("existing-token");
                verify(redisService, never()).sAdd(anyString(), anyString());
        }

        @Test
        @DisplayName("[TC_SE_51] joinSessionExam_Rejoin_MissingToken_GeneratesNew - Sinh viên quay lại nhưng mất token trong Redis")
        void joinSessionExam_Rejoin_MissingToken_GeneratesNew() {
                Long id = 1L;
                User student = User.builder().id(2L).email("student@test.com").build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).build();
                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(LocalDateTime.now().minusMinutes(10))
                                .duration(60L).build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(id)).thenReturn(Optional.of(exam));
                when(redisService.sIsMember(contains("eligible"), eq("2"))).thenReturn(true);
                when(redisService.getExamState(anyString())).thenReturn(state);

                when(redisService.sIsMember(contains("joined"), eq("2"))).thenReturn(true);
                when(redisService.hGet(anyString(), eq(AppConst.FieldConst.SESSION_TOKEN))).thenReturn(null);
                when(redisService.parseString(null)).thenReturn(null);

                JoinExamResponse response = sessionExamService.joinSessionExam(id);

                assertThat(response.getSessionToken()).isNotNull();
                verify(redisService).set(contains("token"), any(), anyLong(), any());
        }

        @Test
        @DisplayName("[TC_SE_52] joinSessionExam_PhaseEnded_ThrowsException - Không cho join khi ca thi đã kết thúc")
        void joinSessionExam_PhaseEnded_ThrowsException() {
                Long id = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).build();
                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(LocalDateTime.now().minusMinutes(100))
                                .duration(60L).build(); // Ended

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findBySessionExamIdAndIsDeletedFalse(id)).thenReturn(Optional.of(exam));
                when(redisService.sIsMember(contains("eligible"), eq("2"))).thenReturn(true);
                when(redisService.getExamState(anyString())).thenReturn(state);

                assertThatThrownBy(() -> sessionExamService.joinSessionExam(id))
                                .isInstanceOf(AppException.class)
                                .hasMessageContaining("Ca thi đã kết thúc");
        }

        @Test
        @DisplayName("[TC_SE_53] submitExam_Success_FullMultiChoice - Chấm điểm câu hỏi Single Choice (MULTI_CHOICE)")
        void submitExam_Success_FullMultiChoice() {
                Long sessionExamId = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).build();
                StudentSessionExam sse = StudentSessionExam.builder()
                                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED)
                                .build();

                // Câu 1: Single Choice (MULTI_CHOICE trong QuestionType enum)
                ExamQuestionSnapshot q1 = new ExamQuestionSnapshot();
                q1.setId(100L);
                q1.setQuestionType(QuestionType.MULTI_CHOICE);
                ExamQuestionAnswerSnapshot a1 = new ExamQuestionAnswerSnapshot();
                a1.setId(1001L);
                a1.setIsCorrect(true);
                q1.setExamQuestionAnswers(List.of(a1));

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);
                ExamSubmissionRequest.AnswerSubmission submission = new ExamSubmissionRequest.AnswerSubmission();
                submission.setQuestionSnapshotId(100L);
                submission.setSelectedAnswerIds(List.of(1001L));
                request.setAnswers(List.of(submission));

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(exam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                                .thenReturn(Optional.of(sse));
                when(examQuestionSnapshotRepository.findAllBySessionExamId(sessionExamId)).thenReturn(List.of(q1));

                var res = sessionExamService.submitExam(request);
                assertThat(res.getScore()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("[TC_SE_54] submitExam_Success_MultipleChoiceLogic - Chấm điểm câu hỏi Multiple Choice (nhiều đáp án đúng)")
        void submitExam_Success_MultipleChoiceLogic() {
                Long sessionExamId = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).build();
                StudentSessionExam sse = StudentSessionExam.builder()
                                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED).build();

                // Câu 1: Logic project bạn QuestionType != MULTI_CHOICE (tức SINGLE_CHOICE) là
                // chấm nhiều đáp án
                ExamQuestionSnapshot q1 = new ExamQuestionSnapshot();
                q1.setId(100L);
                q1.setQuestionType(QuestionType.SINGLE_CHOICE);
                ExamQuestionAnswerSnapshot a1 = new ExamQuestionAnswerSnapshot();
                a1.setId(1001L);
                a1.setIsCorrect(true);
                ExamQuestionAnswerSnapshot a2 = new ExamQuestionAnswerSnapshot();
                a2.setId(1002L);
                a2.setIsCorrect(true);
                q1.setExamQuestionAnswers(Arrays.asList(a1, a2));

                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(sessionExamId);
                ExamSubmissionRequest.AnswerSubmission sub = new ExamSubmissionRequest.AnswerSubmission();
                sub.setQuestionSnapshotId(100L);
                sub.setSelectedAnswerIds(Arrays.asList(1001L, 1002L));
                request.setAnswers(Arrays.asList(sub));

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(sessionExamId)).thenReturn(Optional.of(exam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, 2L))
                                .thenReturn(Optional.of(sse));
                when(examQuestionSnapshotRepository.findAllBySessionExamId(sessionExamId))
                                .thenReturn(Arrays.asList(q1));

                var res = sessionExamService.submitExam(request);
                assertThat(res.getScore()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("[TC_SE_55] submitExam - Chặn nộp bài khi đã nộp rồi (Sử dụng ExamSubmissionRequest)")
        void submitExam_ThrowsForbidden_WhenAlreadySubmitted_ExamRequest() {
                ExamSubmissionRequest request = new ExamSubmissionRequest();
                request.setSessionExamId(1L);
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).build();
                StudentSessionExam sse = StudentSessionExam.builder().submissionStatus(ExamSubmissionStatus.SUBMITTED)
                                .build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(1L)).thenReturn(Optional.of(exam));
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(1L, 2L))
                                .thenReturn(Optional.of(sse));

                assertThatThrownBy(() -> sessionExamService.submitExam(request))
                                .isInstanceOf(AppException.class)
                                .hasMessageContaining("Bạn đã nộp bài thi này rồi");
        }

        @Test
        @DisplayName("[TC_SE_59] getStudentExamOverview_Flexible_Success - Lấy tổng quan bài thi mode FLEXIBLE")
        void getStudentExamOverview_Flexible_Success() {
                Long id = 1L;
                User student = User.builder().id(2L).build();
                SessionExam exam = SessionExam.builder()
                                .sessionExamId(id)
                                .examMode(ExamMode.FLEXIBLE)
                                .startDate(LocalDateTime.now().minusDays(1))
                                .endDate(LocalDateTime.now().plusDays(1))
                                .duration(60L)
                                .build();

                when(authService.getCurrentUser()).thenReturn(student);
                when(sessionExamRepository.findById(id)).thenReturn(Optional.of(exam));
                when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(any(), any(), any(),
                                any()))
                                .thenReturn(Optional.of(new com.vn.backend.entities.ClassMember()));

                var res = sessionExamService.getStudentExamOverview(id);
                assertThat(res.getExamMode()).isEqualTo(ExamMode.FLEXIBLE);
                assertThat(res.getCanStartExam()).isTrue();
        }

        @Test
        @DisplayName("[TC_SE_56] processFlexExamStarted_AutoSubmitsExpired - Tự động nộp bài khi hết giờ trong mode FLEXIBLE")
        void processFlexExamStarted_AutoSubmitsExpired() {
                LocalDateTime now = LocalDateTime.now();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).duration(60L).build();
                StudentSessionExam sse = StudentSessionExam.builder()
                                .sessionExamId(1L)
                                .studentId(2L)
                                .examStartTime(now.minusMinutes(70)) // 70 > 60 -> Expired
                                .sessionExam(exam)
                                .build();

                when(studentSessionExamRepository.findActiveFlexExams(ExamSubmissionStatus.NOT_SUBMITTED))
                                .thenReturn(List.of(sse));
                when(redisService.get(anyString(), eq(ExamQuestionsResponse.class))).thenReturn(null);
                when(examQuestionSnapshotRepository.findAllBySessionExamId(1L)).thenReturn(new ArrayList<>());

                sessionExamService.processFlexExamStarted(now);

                verify(studentSessionExamRepository).saveAndFlush(sse);
                assertThat(sse.getSubmissionStatus()).isEqualTo(ExamSubmissionStatus.SUBMITTED);
        }

        @Test
        @DisplayName("[TC_SE_57] processFlexExamStarted_SavesProgress - Lưu tạm kết quả thi từ Redis vào DB định kỳ")
        void processFlexExamStarted_SavesProgress() {
                LocalDateTime now = LocalDateTime.now();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).duration(60L).build();
                StudentSessionExam sse = StudentSessionExam.builder()
                                .sessionExamId(1L)
                                .studentId(2L)
                                .examStartTime(now.minusMinutes(10)) // Chưa hết giờ
                                .sessionExam(exam)
                                .build();

                ExamQuestionsResponse content = ExamQuestionsResponse.builder().build();
                when(studentSessionExamRepository.findActiveFlexExams(ExamSubmissionStatus.NOT_SUBMITTED))
                                .thenReturn(List.of(sse));
                when(redisService.get(anyString(), eq(ExamQuestionsResponse.class))).thenReturn(content);

                sessionExamService.processFlexExamStarted(now);

                verify(studentSessionExamRepository).saveAndFlush(sse); // saveExamProgressToDb calls saveAndFlush
        }

        @Test
        @DisplayName("[TC_SE_58] processExpiredNotStartedFlexExams_Success - Chấm 0 điểm cho SV không làm bài mà ca thi đã kết thúc")
        void processExpiredNotStartedFlexExams_Success() {
                LocalDateTime now = LocalDateTime.now();
                SessionExam exam = SessionExam.builder().sessionExamId(1L).build();
                StudentSessionExam sse = StudentSessionExam.builder().sessionExamId(1L).studentId(2L)
                                .sessionExam(exam).build();

                when(studentSessionExamRepository.findNotStartedExpiredFlexExamsWithSessionExam(any(), any()))
                                .thenReturn(List.of(sse));
                when(examQuestionSnapshotRepository.findAllBySessionExamId(1L)).thenReturn(new ArrayList<>());

                sessionExamService.processExpiredNotStartedFlexExams(now);

                assertThat(sse.getScore()).isEqualTo(0.0);
                verify(studentSessionExamRepository).saveAndFlush(sse);
        }

        @Test
        @DisplayName("[TC_SE_60] getExamMonitoring_Ended_ReturnsLogData - Lấy dữ liệu giám sát từ Log sau khi thi xong")
        void getExamMonitoring_Ended_ReturnsLogData() {
                Long id = 1L;
                User teacher = User.builder().id(1L).build();
                SessionExam exam = SessionExam.builder().sessionExamId(id).createdBy(1L).status(SessionExamStatus.ENDED)
                                .build();
                SessionExamMonitoringResponse logData = SessionExamMonitoringResponse.builder().title("Log Title")
                                .build();
                SessionExamMonitoringLog log = SessionExamMonitoringLog.builder().monitoringData(logData).build();

                when(authService.getCurrentUser()).thenReturn(teacher);
                when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(id, 1L))
                                .thenReturn(Optional.of(exam));
                when(sessionExamMonitoringLogRepository.findById(id)).thenReturn(Optional.of(log));

                var res = sessionExamService.getExamMonitoring(id);
                assertThat(res.getTitle()).isEqualTo("Log Title");
        }

        @Test
        @DisplayName("[TC_SE_61] getStudentExamResult_Success - Lấy kết quả bài làm thành công")
        void getStudentExamResult_Success_Positive() {
                Long sseId = 1L;
                User student = User.builder().id(2L).build();
                com.vn.backend.dto.response.studentsessionexam.StudentExamResultQueryDTO mockQueryDto = mock(
                                com.vn.backend.dto.response.studentsessionexam.StudentExamResultQueryDTO.class);
                when(mockQueryDto.getStudentSessionExamId()).thenReturn(sseId);

                when(authService.getCurrentUser()).thenReturn(student);
                when(studentSessionExamRepository.getStudentExamResult(sseId, 2L)).thenReturn(mockQueryDto);

                var res = sessionExamService.getStudentExamResult(sseId);
                assertThat(res).isNotNull();
        }

        @Test
        @DisplayName("[TC_SE_62] startLiveSessionExam_Fails_WhenAlreadyPastEndDate - Không cho mở phòng thi khi đã qua thời gian kết thúc")
        void startLiveSessionExam_Fails_WhenAlreadyPastEndDate() {
                SessionExam se = SessionExam.builder()
                                .sessionExamId(1L)
                                .endDate(LocalDateTime.now().minusMinutes(1)) // Đã qua kết thúc
                                .build();
                when(redisService.exists(anyString())).thenReturn(false);
                when(messageUtils.getMessage(anyString())).thenReturn("Already started or expired");

                assertThatThrownBy(() -> sessionExamService.startLiveSessionExam(se))
                                .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("[TC_SE_63] downloadExam_Success_PositivePath - Lấy đề thi thành công trong mode LIVE/STRICT")
        void downloadExam_Success_PositivePath() {
                Long id = 1L;
                String token = "valid-token";
                StudentInfoDTO info = new StudentInfoDTO();
                info.setStudentId(2L);

                SessionExamStateDTO state = SessionExamStateDTO.builder()
                                .countdownStartAt(LocalDateTime.now().minusMinutes(10))
                                .duration(60L)
                                .questionOrderMode(QuestionOrderMode.SEQUENTIAL)
                                .build();

                when(redisService.get(anyString(), eq(StudentInfoDTO.class))).thenReturn(info);
                when(redisService.getExamState(anyString())).thenReturn(state);
                when(redisService.hGetAllAsString(anyString())).thenReturn(Map.of("status", "JOINED"));

                // Mock Questions Content
                SessionExamContentDTO content = new SessionExamContentDTO();
                content.setQuestions(new ArrayList<>());
                when(redisService.get(contains("content"), eq(SessionExamContentDTO.class))).thenReturn(content);

                // Mock Handle First Download logic
                StudentSessionExam sse = StudentSessionExam.builder().build();
                when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(id, 2L))
                                .thenReturn(Optional.of(sse));

                var res = sessionExamService.downloadExam(id, token);
                assertThat(res.getSessionExamId()).isEqualTo(id);
                verify(studentSessionExamRepository).save(sse); // handleFirstDownload calls save
        }

        // ============ Helper ============
        private SearchRequest createPagnination() {
                SearchRequest s = new SearchRequest();
                s.setPageNum("1");
                s.setPageSize("10");
                return s;
        }
}
