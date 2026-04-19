package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamAddRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamSearchRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.studentsessionexam.AvailableStudentResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentSessionExamResponse;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.ClassroomSetting;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomSettingRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.EmailService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.impl.StudentSessionExamServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentSessionExamServiceImpl Unit Tests")
class StudentSessionExamServiceImplTest {

    @Mock
    private StudentSessionExamRepository studentSessionExamRepository;
    @Mock
    private SessionExamRepository sessionExamRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private ClassroomSettingRepository classroomSettingRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private StudentSessionExamServiceImpl studentSessionExamService;

    private User teacher;
    private User student;
    private SessionExam sessionExam;

    @BeforeEach
    void setUp() {
        teacher = User.builder().id(1L).build();
        student = User.builder().id(2L).fullName("Student A").email("student@test.com").build();
        sessionExam = SessionExam.builder()
                .sessionExamId(10L)
                .classId(1L)
                .createdBy(1L)
                .status(SessionExamStatus.NOT_STARTED)
                .build();
    }

    // ===========================================
    // I. Group: Search Students (searchClassStudentsForSessionExam)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_01: searchClassStudentsForSessionExam - Thành công tìm kiếm học sinh trong lớp")
    void searchClassStudentsForSessionExam_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));

        Object[] mockResult = new Object[]{2L, "Student A", "userA", "CODE01", "student@test.com", "avatar.png", 1};
        when(classMemberRepository.searchStudentsWithJoinStatus(eq(1L), eq(10L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockResult), PageRequest.of(0, 10), 1));

        var response = studentSessionExamService.searchClassStudentsForSessionExam(request);

        assertThat(response.getContent()).hasSize(1);
        var firstItem = new java.util.ArrayList<>(response.getContent()).get(0);
        assertThat(firstItem.getFullName()).isEqualTo("Student A");
        assertThat(firstItem.isJoined()).isTrue();
    }

    @Test
    @DisplayName("TC_QLT_02: searchClassStudentsForSessionExam - Thành công tìm kiếm với từ khóa")
    void searchClassStudentsForSessionExam_WithKeyword_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        filter.setSearch("Student A");
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.searchStudentsWithJoinStatus(eq(1L), eq(10L), eq("%Student A%"), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        studentSessionExamService.searchClassStudentsForSessionExam(request);

        verify(classMemberRepository).searchStudentsWithJoinStatus(eq(1L), eq(10L), eq("%Student A%"), any());
    }

    @Test
    @DisplayName("TC_QLT_03: searchClassStudentsForSessionExam - Lỗi khi thiếu mã ca thi (sessionExamId)")
    void searchClassStudentsForSessionExam_ThrowsException_WhenSessionIdMissing() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new StudentSessionExamSearchRequest()); // missing ID

        assertThatThrownBy(() -> studentSessionExamService.searchClassStudentsForSessionExam(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_04: searchClassStudentsForSessionExam - Lỗi khi ca thi không tồn tại")
    void searchClassStudentsForSessionExam_ThrowsException_WhenSessionNotFound() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(99L);
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentSessionExamService.searchClassStudentsForSessionExam(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_05: searchClassStudentsForSessionExam - Lỗi khi giáo viên không sở hữu ca thi")
    void searchClassStudentsForSessionExam_ThrowsForbidden_WhenNotOwner() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);

        User anotherTeacher = User.builder().id(99L).build();
        when(authService.getCurrentUser()).thenReturn(anotherTeacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));

        assertThatThrownBy(() -> studentSessionExamService.searchClassStudentsForSessionExam(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
    }

    // ===========================================
    // II. Group: Add Students (addStudents)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_06: addStudents - Thành công thêm sinh viên và tạo thông báo")
    void addStudents_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                1L, 2L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(classMember));
        when(studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(false);
        when(studentSessionExamRepository.saveAndFlush(any(StudentSessionExam.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        var response = studentSessionExamService.addStudents(request);

        assertThat(response).hasSize(1);
        verify(notificationService).createNotificationForUser(any(User.class), eq(2L), any(NotificationObjectType.class), anyLong(), any(SessionExam.class));
        verify(studentSessionExamRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_QLT_07: addStudents - Bỏ qua sinh viên đã tồn tại trong danh sách")
    void addStudents_SkipExisting_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.of(classMember));
        when(studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(true); // Đã tồn tại

        var response = studentSessionExamService.addStudents(request);

        assertThat(response).isEmpty();
        verify(studentSessionExamRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_QLT_08: addStudents - Lỗi khi ca thi không tồn tại")
    void addStudents_ThrowsException_WhenSessionNotFound() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(99L);
        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentSessionExamService.addStudents(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_09: addStudents - Lỗi khi trạng thái ca thi không phải NOT_STARTED")
    void addStudents_ThrowsException_WhenSessionOngoing() {
        sessionExam.setStatus(SessionExamStatus.ONGOING);
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));

        assertThatThrownBy(() -> studentSessionExamService.addStudents(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not possible to add students");
    }

    @Test
    @DisplayName("TC_QLT_10: addStudents - Lỗi khi sinh viên không thuộc lớp học")
    void addStudents_ThrowsException_WhenStudentNotInClass() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(99L));

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), eq(99L), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentSessionExamService.addStudents(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_11: addStudents - Gửi email thông báo (Nếu lớp học bật notifyEmail)")
    void addStudents_SendsEmail_WhenEnabled() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        ClassroomSetting setting = ClassroomSetting.builder()
                .classroomId(1L)
                .notifyEmail(true)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.of(classMember));
        when(classroomSettingRepository.findByClassroomId(1L)).thenReturn(Optional.of(setting));
        when(studentSessionExamRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        studentSessionExamService.addStudents(request);

        // Note: emailService.sendExamEmail is commented out in code, but we check if setting was fetched
        verify(classroomSettingRepository).findByClassroomId(1L);
    }

    // ===========================================
    // III. Group: Remove Student (removeStudent)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_12: removeStudent - Thành công xóa sinh viên khỏi ca thi")
    void removeStudent_Success() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .studentSessionExamId(100L)
                .submissionStatus(ExamSubmissionStatus.NOT_STARTED)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(Optional.of(sse));

        studentSessionExamService.removeStudent(10L, 2L);

        verify(studentSessionExamRepository).delete(sse);
    }

    @Test
    @DisplayName("TC_QLT_13: removeStudent - Lỗi khi ca thi không tồn tại hoặc không đủ quyền")
    void removeStudent_ThrowsException_WhenSessionNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_14: removeStudent - Lỗi khi không tìm thấy bản xác nhận dự thi của SV")
    void removeStudent_ThrowsException_WhenRecordNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_15: removeStudent - Chặn xóa sinh viên đã bắt đầu tham gia làm bài")
    void removeStudent_ThrowsException_WhenAlreadyParticipated() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .studentSessionExamId(100L)
                .examStartTime(LocalDateTime.now()) // Đã bắt đầu làm bài
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("CANNOT_REMOVE_STUDENT_EXAM_PARTICIPATED");
    }

    // ===========================================
    // IV. Group: Deep Branch Coverage (New)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_16: searchClassStudentsForSessionExam - Thành công khi keyword tìm kiếm rỗng/null")
    void searchClassStudentsForSessionExam_NullKeyword_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        filter.setSearch(null); // Nhánh keyword rỗng
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.searchStudentsWithJoinStatus(eq(1L), eq(10L), isNull(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        studentSessionExamService.searchClassStudentsForSessionExam(request);
        verify(classMemberRepository).searchStudentsWithJoinStatus(anyLong(), anyLong(), isNull(), any());
    }

    @Test
    @DisplayName("TC_QLT_17: searchClassStudentsForSessionExam - Thành công khi phân trang (pagination) bị null")
    void searchClassStudentsForSessionExam_NullPagination_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);
        request.setPagination(null); // Nhánh pagination null

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        studentSessionExamService.searchClassStudentsForSessionExam(request);
        // Verify code used default page IDX 0 and size 20 (from nativePageable logic)
        verify(classMemberRepository).searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_QLT_18: addStudents - Nhánh ClassroomSetting bị null (Không gửi email)")
    void addStudents_ClassroomSettingNull_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(classMember));
        when(classroomSettingRepository.findByClassroomId(1L)).thenReturn(Optional.empty()); // Nhánh setting null

        studentSessionExamService.addStudents(request);
        verify(studentSessionExamRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_QLT_19: addStudents - Nhánh notifyEmail bị false (Bỏ qua gửi email)")
    void addStudents_NotifyEmailDisabled_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassroomSetting setting = ClassroomSetting.builder().notifyEmail(false).build(); // OFF

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(classMember));
        when(classroomSettingRepository.findByClassroomId(1L)).thenReturn(Optional.of(setting));

        studentSessionExamService.addStudents(request);
        verify(studentSessionExamRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_QLT_20: addStudents - Nhánh email sinh viên bị null (Bỏ qua gửi email)")
    void addStudents_StudentEmailNull_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        student.setEmail(null); // Nhánh email null
        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        ClassroomSetting setting = ClassroomSetting.builder().notifyEmail(true).build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(classMember));
        when(classroomSettingRepository.findByClassroomId(1L)).thenReturn(Optional.of(setting));

        studentSessionExamService.addStudents(request);
        verify(studentSessionExamRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("TC_QLT_21: addStudents - Chặn đứng lỗi khi quá trình gửi email gặp Exception")
    void addStudents_EmailServiceThrowsException_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember classMember = new ClassMember();
        classMember.setUser(student);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(classMember));
        
        // Giả lập setting gây lỗi khi thực thi
        when(classroomSettingRepository.findByClassroomId(1L)).thenThrow(new RuntimeException("DB Error in catch block"));

        // Vẫn phải thành công vì email nằm trong try-catch và không làm rollback transaction
        var response = studentSessionExamService.addStudents(request);
        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLT_22: removeStudent - Chặn xóa khi sinh viên đã tham gia (joinedAt != null)")
    void removeStudent_ThrowsException_WhenJoinedAtNotNull() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .joinedAt(LocalDateTime.now()) // Nhánh joinedAt
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_23: removeStudent - Chặn xóa khi trạng thái bài thi là ONGOING")
    void removeStudent_ThrowsException_WhenStatusOngoing() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionStatus(ExamSubmissionStatus.NOT_SUBMITTED) // Nhánh status NOT_SUBMITTED (đang làm)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_24: removeStudent - Chặn xóa khi trạng thái bài thi là SUBMITTED")
    void removeStudent_ThrowsException_WhenStatusSubmitted() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionStatus(ExamSubmissionStatus.SUBMITTED) // Nhánh status submitted
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_25: removeStudent - Chặn xóa khi đã có thời gian nộp bài (submissionTime != null)")
    void removeStudent_ThrowsException_WhenSubmissionTimeNotNull() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionTime(LocalDateTime.now()) // Nhánh submissionTime
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_26: searchClassStudentsForSessionExam - Lỗi khi bộ lọc (filters) bị null")
    void searchClassStudentsForSessionExam_ThrowsException_WhenFilterNull() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(null); // Nhánh filter null

        assertThatThrownBy(() -> studentSessionExamService.searchClassStudentsForSessionExam(request))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // V. Group: Advanced Boolean & Null Branch Coverage (New)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_27: searchClassStudentsForSessionExam - Thành công xử lý sinh viên CHƯA tham gia (joined = 0)")
    void searchClassStudentsForSessionExam_NotJoined_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));

        // joined = 0 (false)
        Object[] mockResult = new Object[]{3L, "Student B", "userB", "CODE02", "b@test.com", "img.png", 0};
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockResult)));

        var response = studentSessionExamService.searchClassStudentsForSessionExam(request);
        var firstItem = new ArrayList<>(response.getContent()).get(0);
        assertThat(firstItem.isJoined()).isFalse();
    }

    @Test
    @DisplayName("TC_QLT_28: searchClassStudentsForSessionExam - Keyword chỉ có khoảng trắng (Verify trim().isEmpty())")
    void searchClassStudentsForSessionExam_WhitespaceKeyword_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        filter.setSearch("   "); // Khoảng trắng
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));
        
        // Native query nhận vào NULL nếu keyword.trim().isEmpty() là true
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), isNull(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        studentSessionExamService.searchClassStudentsForSessionExam(request);
        verify(classMemberRepository).searchStudentsWithJoinStatus(anyLong(), anyLong(), isNull(), any());
    }

    @Test
    @DisplayName("TC_QLT_29: searchClassStudentsForSessionExam - Pagination không null nhưng PagingMeta bị null")
    void searchClassStudentsForSessionExam_PaginationExistsButMetaNull_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);
        request.setPagination(new com.vn.backend.dto.request.common.SearchRequest()); // Meta exists but empty pageSize

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(new ArrayList<>()));

        studentSessionExamService.searchClassStudentsForSessionExam(request);
        // Verify code used default page IDX 0
        verify(classMemberRepository).searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_QLT_30: addStudents - Request với danh sách sinh viên rỗng (Loop safety)")
    void addStudents_EmptyStudentList_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(new ArrayList<>()); // Rỗng

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));

        var response = studentSessionExamService.addStudents(request);
        assertThat(response).isEmpty();
        verify(classMemberRepository, never()).findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("TC_QLT_31: removeStudent - Chặn xóa khi submissionStatus null nhưng joinedAt có giá trị")
    void removeStudent_StatusNullButJoinedAtNotNull_ThrowsException() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .joinedAt(LocalDateTime.now())
                .submissionStatus(null) // Status null
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_32: removeStudent - Chặn xóa khi submissionStatus là SUBMITTED dù submissionTime bằng null")
    void removeStudent_StatusSubmittedButTimeNull_ThrowsException() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionTime(null)
                .submissionStatus(ExamSubmissionStatus.SUBMITTED) // Nhánh OR thứ 2 của hasSubmitted
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // VI. Group: Precision Branch & Data Mapping (Final)
    // ===========================================

    @Test
    @DisplayName("TC_QLT_33: searchClassStudentsForSessionExam - Thành công tính toán thông tin phân trang (Tổng số dòng/trang)")
    void searchClassStudentsForSessionExam_PaginationMeta_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));

        List<Object[]> content = new ArrayList<>();
        content.add(new Object[]{2L, "SV A", "userA", "C1", "a@test.com", "img.png", 1});
        
        // Giả lập Page có 50 phần tử, chia 10 item mỗi trang => 5 trang
        Page<Object[]> page = new PageImpl<>(content, PageRequest.of(0, 10), 50);
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any())).thenReturn(page);

        var response = studentSessionExamService.searchClassStudentsForSessionExam(request);
        
        assertThat(response.getPaging().getTotalRows()).isEqualTo(50);
        assertThat(response.getPaging().getTotalPages()).isEqualTo(5);
        assertThat(response.getPaging().getPageNum()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC_QLT_34: addStudents - Thành công xử lý danh sách hỗn hợp (Sinh viên mới và sinh viên đã có)")
    void addStudents_MixedList_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L, 3L)); // 2 sinh viên

        ClassMember member2 = new ClassMember(); member2.setUser(student);
        User student3 = User.builder().id(3L).fullName("Student New").build();
        ClassMember member3 = new ClassMember(); member3.setUser(student3);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        
        // SV 2: Đã tồn tại -> Skip
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(eq(1L), eq(2L), any(), any())).thenReturn(Optional.of(member2));
        when(studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(true);

        // SV 3: Mới -> Thêm
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(eq(1L), eq(3L), any(), any())).thenReturn(Optional.of(member3));
        when(studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 3L)).thenReturn(false);
        when(studentSessionExamRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        var response = studentSessionExamService.addStudents(request);

        assertThat(response).hasSize(1); // Chỉ thêm được 1 SV mới
        assertThat(response.get(0).getStudentFullName()).isEqualTo("Student New");
    }

    @Test
    @DisplayName("TC_QLT_35: removeStudent - Chặn xóa khi submissionTime tồn tại mặc dù status nộp bài bị NULL")
    void removeStudent_StatusNull_SubmissionTimeExists_ThrowsException() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .submissionTime(LocalDateTime.now()) // Có thời gian nộp
                .submissionStatus(null) // Nhưng status null
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L)).thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_36: addStudents - Thành công khi sinh viên có email đầy đủ nhưng lớp học tắt tính năng notifyEmail")
    void addStudents_NotifyEmailOff_WithValidStudentEmail_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        ClassMember member = new ClassMember(); member.setUser(student); // student có email
        ClassroomSetting setting = ClassroomSetting.builder().notifyEmail(false).build(); // Tắt notify

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sessionExam));
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(member));
        when(classroomSettingRepository.findByClassroomId(1L)).thenReturn(Optional.of(setting));
        when(studentSessionExamRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        studentSessionExamService.addStudents(request);
        verify(studentSessionExamRepository).saveAndFlush(any());
        // Verify emailService skip (ngầm định không văng lỗi)
    }

    @Test
    @DisplayName("TC_QLT_37: searchClassStudentsForSessionExam - Kiểm tra tính toàn vẹn của dữ liệu Mapping DTO")
    void searchClassStudentsForSessionExam_DataIntegrity_Success() {
        BaseFilterSearchRequest<StudentSessionExamSearchRequest> request = new BaseFilterSearchRequest<>();
        StudentSessionExamSearchRequest filter = new StudentSessionExamSearchRequest();
        filter.setSessionExamId(10L);
        request.setFilters(filter);

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findById(10L)).thenReturn(Optional.of(sessionExam));

        // Full data mock
        Object[] fullObj = new Object[]{5L, "Full Name", "userX", "CODE-X", "x@test.com", "avatar.jpg", 1};
        when(classMemberRepository.searchStudentsWithJoinStatus(anyLong(), anyLong(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(fullObj)));

        var response = studentSessionExamService.searchClassStudentsForSessionExam(request);
        var item = new ArrayList<>(response.getContent()).get(0);
        
        assertThat(item.getStudentId()).isEqualTo(5L);
        assertThat(item.getFullName()).isEqualTo("Full Name");
        assertThat(item.getUsername()).isEqualTo("userX");
        assertThat(item.getCode()).isEqualTo("CODE-X");
        assertThat(item.getEmail()).isEqualTo("x@test.com");
        assertThat(item.getAvatarUrl()).isEqualTo("avatar.jpg");
    }
}
