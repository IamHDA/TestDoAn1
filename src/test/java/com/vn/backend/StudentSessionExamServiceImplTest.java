package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamAddRequest;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.SessionExamStatus;
import com.vn.backend.enums.ExamSubmissionStatus;
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
import org.springframework.http.HttpStatus;

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
    private SessionExam sessionExam;

    @BeforeEach
    void setUp() {
        teacher = User.builder().id(1L).build();
        sessionExam = SessionExam.builder()
                .sessionExamId(10L)
                .classId(1L)
                .createdBy(1L)
                .status(SessionExamStatus.NOT_STARTED)
                .build();
    }

    @Test
    @DisplayName("addStudents - thành công thêm sinh viên vào ca thi")
    void addStudents_Success() {
        StudentSessionExamAddRequest request = new StudentSessionExamAddRequest();
        request.setSessionExamId(10L);
        request.setStudentIds(List.of(2L));

        User student = User.builder().id(2L).fullName("Student Name").build();
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
        assertThat(response.get(0).getStudentFullName()).isEqualTo("Student Name");
        verify(notificationService).createNotificationForUser(eq(teacher), eq(2L), any(), anyLong(), eq(sessionExam));
    }

    @Test
    @DisplayName("removeStudent - ném exception khi sinh viên đã bắt đầu làm bài")
    void removeStudent_ThrowsException_WhenStudentStarted() {
        StudentSessionExam sse = StudentSessionExam.builder()
                .studentSessionExamId(100L)
                .examStartTime(java.time.LocalDateTime.now())
                .build();

        when(authService.getCurrentUser()).thenReturn(teacher);
        when(sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sessionExam));
        when(studentSessionExamRepository.findBySessionExamIdAndStudentIdAndIsDeletedFalse(10L, 2L))
                .thenReturn(Optional.of(sse));

        assertThatThrownBy(() -> studentSessionExamService.removeStudent(10L, 2L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("removeStudent - thành công xóa sinh viên chưa tham gia thi")
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
}
