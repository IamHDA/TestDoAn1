package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.entities.Exam;
import com.vn.backend.entities.ExamQuestion;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ExamQuestionRepository;
import com.vn.backend.repositories.ExamRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.ExamServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamServiceImpl Unit Tests")
class ExamServiceImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private ExamServiceImpl examService;

    private User teacherUser;
    private Exam existingExam;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .username("teacher01")
                .email("teacher@example.com")
                .role(Role.TEACHER)
                .isActive(true)
                .isDeleted(false)
                .build();

        existingExam = Exam.builder()
                .examId(10L)
                .subjectId(1L)
                .subject(com.vn.backend.entities.Subject.builder().subjectId(1L).build())
                .title("Test Exam")
                .description("Test Description")
                .createdBy(1L)
                .isDeleted(false)
                .build();
    }

    // ===================== getExam =====================

    @Test
    @DisplayName("getExam - thành công khi exam tồn tại và thuộc về user")
    void getExam_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));

        var result = examService.getExam("10");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getExam - ném exception khi exam không tồn tại")
    void getExam_ThrowsException_WhenExamNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(999L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> examService.getExam("999"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ===================== deleteExam =====================

    @Test
    @DisplayName("deleteExam - thành công khi soft delete exam")
    void deleteExam_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));

        examService.deleteExam("10");

        assertThat(existingExam.getIsDeleted()).isTrue();
        verify(examRepository).save(existingExam);
    }

    @Test
    @DisplayName("deleteExam - ném exception khi exam không tìm thấy")
    void deleteExam_ThrowsException_WhenExamNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(999L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> examService.deleteExam("999"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(examRepository, never()).save(any());
    }

    // ===================== duplicateExam =====================

    @Test
    @DisplayName("duplicateExam - thành công, tạo exam mới với COPY suffix")
    void duplicateExam_Success() {
        ExamQuestion eq1 = ExamQuestion.builder()
                .examId(10L)
                .questionId(1L)
                .orderIndex(0)
                .score(1.0)
                .build();

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam saved = inv.getArgument(0);
            saved = Exam.builder()
                    .examId(99L)
                    .subjectId(saved.getSubjectId())
                    .title(saved.getTitle())
                    .description(saved.getDescription())
                    .createdBy(saved.getCreatedBy())
                    .isDeleted(false)
                    .build();
            return saved;
        });
        when(examQuestionRepository.getAllExamQuestion(10L, 1L)).thenReturn(List.of(eq1));

        examService.duplicateExam("10");

        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        Exam newExam = examCaptor.getValue();
        assertThat(newExam.getTitle()).contains(AppConst.COPY);
        assertThat(newExam.getDescription()).contains(AppConst.COPY);
        verify(examQuestionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("duplicateExam - ném exception khi exam gốc không tìm thấy")
    void duplicateExam_ThrowsException_WhenOriginalExamNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(999L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> examService.duplicateExam("999"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                });
    }

    // ===================== getExamStatistic =====================

    @Test
    @DisplayName("getExamStatistic - thành công, trả về thống kê câu hỏi")
    void getExamStatistic_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.countQuestions(10L, 1L)).thenReturn(5L);
        when(examQuestionRepository.countByDifficulty(10L, 1L)).thenReturn(List.of());
        when(examQuestionRepository.countByTopic(10L, 1L)).thenReturn(List.of());

        var result = examService.getExamStatistic("10");

        assertThat(result).isNotNull();
        assertThat(result.getTotalQuestion()).isEqualTo(5);
    }
}
