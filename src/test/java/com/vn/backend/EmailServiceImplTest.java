package com.vn.backend;

import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.services.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Unit Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User student;
    private Classroom classroom;
    private SessionExam sessionExam;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        // Inject properties using ReflectionTestUtils since they are @Value annotated
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@classsystem.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");

        student = User.builder()
                .id(1L)
                .email("student@example.com")
                .fullName("John Doe")
                .build();

        classroom = Classroom.builder()
                .classroomId(100L)
                .className("Math 101")
                .build();

        sessionExam = SessionExam.builder()
                .sessionExamId(10L)
                .classId(100L)
                .title("Midterm Exam")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .duration(60L)
                .classroom(classroom)
                .build();

        assignment = Assignment.builder()
                .assignmentId(20L)
                .classroomId(100L)
                .title("Homework 1")
                .dueDate(LocalDateTime.now().plusDays(3))
                .classroom(classroom)
                .build();
    }

    // ===================== sendExamEmail =====================

    @Test
    @DisplayName("sendExamEmail - thành công tạo và gửi email")
    void sendExamEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendExamEmail(sessionExam, student, "http://localhost:3000");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendExamEmail - thử nghiệm với sessionExam không có classroom")
    void sendExamEmail_Success_WithNullClassroom() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        sessionExam.setClassroom(null); // Để test fallback "Lớp học"

        emailService.sendExamEmail(sessionExam, student, "http://localhost:3000");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendExamEmail - xử lý khi ném MessagingException")
    void sendExamEmail_CatchMessagingException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // Mocking the overloaded setSubject(String, String) method used by MimeMessageHelper
        doThrow(new MessagingException("Simulated Messaging Error")).when(mimeMessage).setSubject(anyString(), anyString());
        
        emailService.sendExamEmail(sessionExam, student, "http://localhost:3000");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ===================== sendAssignmentCreatedEmail =====================

    @Test
    @DisplayName("sendAssignmentCreatedEmail - thành công gửi email cho danh sách sinh viên")
    void sendAssignmentCreatedEmail_Success() {
        User student2 = User.builder().id(2L).email("student2@example.com").fullName("Jane Doe").build();
        List<User> students = List.of(student, student2);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAssignmentCreatedEmail(assignment, students, "http://localhost:3000");

        verify(mailSender, times(2)).createMimeMessage();
        verify(mailSender, times(2)).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAssignmentCreatedEmail - trường hợp danh sách sinh viên rỗng")
    void sendAssignmentCreatedEmail_EmptyList() {
        emailService.sendAssignmentCreatedEmail(assignment, List.of(), "http://localhost:3000");
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("sendAssignmentCreatedEmail - trường hợp classroom và dueDate bị null")
    void sendAssignmentCreatedEmail_NullFields() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        assignment.setClassroom(null);
        assignment.setDueDate(null);

        emailService.sendAssignmentCreatedEmail(assignment, List.of(student), "http://localhost:3000");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendAssignmentCreatedEmail - xử lý khi ném MessagingException")
    void sendAssignmentCreatedEmail_CatchMessagingException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("Error")).when(mimeMessage).setSubject(anyString(), anyString());

        emailService.sendAssignmentCreatedEmail(assignment, List.of(student), "http://localhost:3000");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ===================== sendReminderEmail =====================

    @Test
    @DisplayName("sendReminderEmail - thành công gửi email nhắc nhở bài tập")
    void sendReminderEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendReminderEmail(student, assignment, 2, "http://localhost:3000");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendReminderEmail - xử lý khi ném MessagingException")
    void sendReminderEmail_CatchMessagingException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("Reminder failed")).when(mimeMessage).setSubject(anyString(), anyString());

        emailService.sendReminderEmail(student, assignment, 2, "http://localhost:3000");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ===================== sendExamReminderEmail =====================

    @Test
    @DisplayName("sendExamReminderEmail - thành công gửi email nhắc nhở bài thi")
    void sendExamReminderEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendExamReminderEmail(student, sessionExam, 3, "http://localhost:3000");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendReminderEmail - trường hợp classroom bị null và quá hạn 1 ngày")
    void sendReminderEmail_OneDayOverdue_NullClassroom() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        assignment.setClassroom(null);

        emailService.sendReminderEmail(student, assignment, 1, "http://localhost:3000");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendReminderEmail - trường hợp quá hạn >= 3 ngày (màu đỏ)")
    void sendReminderEmail_MultipleDaysOverdue() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        emailService.sendReminderEmail(student, assignment, 3, "http://localhost:3000");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendExamReminderEmail - trường hợp classroom bị null và quá hạn 1 ngày")
    void sendExamReminderEmail_OneDayOverdue_NullClassroom() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        sessionExam.setClassroom(null);

        emailService.sendExamReminderEmail(student, sessionExam, 1, "http://localhost:3000");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendExamReminderEmail - trường hợp quá hạn >= 3 ngày")
    void sendExamReminderEmail_MultipleDays_Exception() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("Failed")).when(mimeMessage).setSubject(anyString(), anyString());

        emailService.sendExamReminderEmail(student, sessionExam, 5, "http://localhost:3000");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
