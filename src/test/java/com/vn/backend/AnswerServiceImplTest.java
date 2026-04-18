package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.answer.AnswerUpdateRequest;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.entities.Answer;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.User;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.AnswerServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerServiceImpl Unit Tests")
class AnswerServiceImplTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AuthService authService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private AnswerServiceImpl answerService;

    private User currentUser;
    private Question singleChoiceQuestion;
    private Question multiChoiceQuestion;
    private Answer existingAnswer;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("teacher")
                .role(Role.TEACHER)
                .isActive(true)
                .isDeleted(false)
                .build();

        singleChoiceQuestion = Question.builder()
                .questionId(10L)
                .content("Single choice question")
                .type(QuestionType.SINGLE_CHOICE)
                .topicId(1L)
                .createdBy(1L)
                .isDeleted(false)
                .build();

        multiChoiceQuestion = Question.builder()
                .questionId(20L)
                .content("Multi choice question")
                .type(QuestionType.MULTI_CHOICE)
                .topicId(1L)
                .createdBy(1L)
                .isDeleted(false)
                .build();

        existingAnswer = Answer.builder()
                .answerId(100L)
                .content("Answer content")
                .questionId(10L)
                .isCorrect(true)
                .displayOrder(0)
                .isDeleted(false)
                .build();
    }

    // ===================== addAnswer =====================

    @Test
    @DisplayName("addAnswer - thành công với câu hỏi MULTI_CHOICE")
    void addAnswer_Success_MultiChoice() {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(20L);
        request.setContent("Answer A");
        request.setIsCorrect(true);
        request.setDisplayOrder(0);

        Answer savedAnswer = Answer.builder()
                .answerId(200L)
                .content("Answer A")
                .questionId(20L)
                .isCorrect(true)
                .displayOrder(0)
                .isDeleted(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(20L, 1L))
                .thenReturn(Optional.of(multiChoiceQuestion));
        when(answerRepository.saveAndFlush(any(Answer.class))).thenReturn(savedAnswer);

        AnswerResponse result = answerService.addAnswer(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getContent()).isEqualTo("Answer A");
        assertThat(result.getIsCorrect()).isTrue();
    }

    @Test
    @DisplayName("addAnswer - thành công với câu hỏi SINGLE_CHOICE khi chưa có đáp án đúng")
    void addAnswer_Success_SingleChoice_NoExistingCorrectAnswer() {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(10L);
        request.setContent("Only correct answer");
        request.setIsCorrect(true);
        request.setDisplayOrder(0);

        Answer savedAnswer = Answer.builder()
                .answerId(101L)
                .content("Only correct answer")
                .questionId(10L)
                .isCorrect(true)
                .displayOrder(0)
                .isDeleted(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(singleChoiceQuestion));
        when(answerRepository.countByQuestionIdAndIsCorrectTrue(10L)).thenReturn(0L);
        when(answerRepository.saveAndFlush(any(Answer.class))).thenReturn(savedAnswer);

        AnswerResponse result = answerService.addAnswer(request);

        assertThat(result).isNotNull();
        assertThat(result.getIsCorrect()).isTrue();
    }

    @Test
    @DisplayName("addAnswer - ném exception khi câu hỏi không tồn tại hoặc không thuộc về user")
    void addAnswer_ThrowsException_WhenQuestionNotFound() {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(999L);
        request.setContent("Answer");
        request.setIsCorrect(false);
        request.setDisplayOrder(0);

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> answerService.addAnswer(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(answerRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("addAnswer - ném exception khi SINGLE_CHOICE đã có hơn 1 đáp án đúng")
    void addAnswer_ThrowsException_WhenSingleChoiceAlreadyHasMoreThanOneCorrectAnswer() {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(10L);
        request.setContent("New correct answer");
        request.setIsCorrect(true);
        request.setDisplayOrder(2);

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(singleChoiceQuestion));
        when(answerRepository.countByQuestionIdAndIsCorrectTrue(10L)).thenReturn(2L);
        when(messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION)).thenReturn("Invalid logic");

        assertThatThrownBy(() -> answerService.addAnswer(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_LOGIC_QUESTION);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("addAnswer - thành công khi đáp án không đúng cho SINGLE_CHOICE")
    void addAnswer_Success_SingleChoice_WrongAnswer() {
        AnswerCreateRequest request = new AnswerCreateRequest();
        request.setQuestionId(10L);
        request.setContent("Wrong answer");
        request.setIsCorrect(false);
        request.setDisplayOrder(1);

        Answer savedAnswer = Answer.builder()
                .answerId(102L)
                .content("Wrong answer")
                .questionId(10L)
                .isCorrect(false)
                .displayOrder(1)
                .isDeleted(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(singleChoiceQuestion));
        when(answerRepository.saveAndFlush(any(Answer.class))).thenReturn(savedAnswer);

        AnswerResponse result = answerService.addAnswer(request);

        assertThat(result).isNotNull();
        assertThat(result.getIsCorrect()).isFalse();
        // Không cần check countByQuestionIdAndIsCorrectTrue khi isCorrect = false
        verify(answerRepository, never()).countByQuestionIdAndIsCorrectTrue(anyLong());
    }

    // ===================== updateAnswer =====================

    @Test
    @DisplayName("updateAnswer - thành công khi cập nhật nội dung")
    void updateAnswer_Success_UpdateContent() {
        AnswerUpdateRequest request = new AnswerUpdateRequest();
        request.setContent("Updated answer content");

        Answer updatedAnswer = Answer.builder()
                .answerId(100L)
                .content("Updated answer content")
                .questionId(10L)
                .isCorrect(true)
                .displayOrder(0)
                .isDeleted(false)
                .build();

        when(answerRepository.findByAnswerIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(existingAnswer));
        when(answerRepository.saveAndFlush(any(Answer.class))).thenReturn(updatedAnswer);

        AnswerResponse result = answerService.updateAnswer(100L, request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated answer content");
    }

    @Test
    @DisplayName("updateAnswer - ném exception khi answer không tồn tại")
    void updateAnswer_ThrowsException_WhenAnswerNotFound() {
        AnswerUpdateRequest request = new AnswerUpdateRequest();
        request.setContent("Updated content");

        when(answerRepository.findByAnswerIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> answerService.updateAnswer(999L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("updateAnswer - ném exception khi bỏ đáp án đúng duy nhất")
    void updateAnswer_ThrowsException_WhenRemovingLastCorrectAnswer() {
        existingAnswer.setIsCorrect(true);

        AnswerUpdateRequest request = new AnswerUpdateRequest();
        request.setIsCorrect(false); // muốn bỏ đáp án đúng

        when(answerRepository.findByAnswerIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(existingAnswer));
        when(answerRepository.countByQuestionIdAndIsCorrectTrueAndAnswerIdNotAndIsDeletedFalse(10L, 100L))
                .thenReturn(0L); // không có đáp án đúng nào khác
        when(messageUtils.getMessage(AppConst.MessageConst.INVALID_LOGIC_QUESTION)).thenReturn("Invalid logic");

        assertThatThrownBy(() -> answerService.updateAnswer(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_LOGIC_QUESTION);
                });
    }

    @Test
    @DisplayName("updateAnswer - thành công khi bỏ đáp án đúng nhưng vẫn còn đáp án đúng khác")
    void updateAnswer_Success_WhenRemovingCorrectAnswerButOthersExist() {
        existingAnswer.setIsCorrect(true);

        AnswerUpdateRequest request = new AnswerUpdateRequest();
        request.setIsCorrect(false);

        Answer updatedAnswer = Answer.builder()
                .answerId(100L)
                .content("Answer content")
                .questionId(10L)
                .isCorrect(false)
                .displayOrder(0)
                .isDeleted(false)
                .build();

        when(answerRepository.findByAnswerIdAndIsDeletedFalse(100L))
                .thenReturn(Optional.of(existingAnswer));
        when(answerRepository.countByQuestionIdAndIsCorrectTrueAndAnswerIdNotAndIsDeletedFalse(10L, 100L))
                .thenReturn(1L); // còn đáp án đúng khác
        when(answerRepository.saveAndFlush(any(Answer.class))).thenReturn(updatedAnswer);

        AnswerResponse result = answerService.updateAnswer(100L, request);

        assertThat(result).isNotNull();
        assertThat(result.getIsCorrect()).isFalse();
    }

    // ===================== softDeleteAnswer =====================

    @Test
    @DisplayName("softDeleteAnswer - thành công khi answer tồn tại")
    void softDeleteAnswer_Success() {
        when(answerRepository.findById(100L)).thenReturn(Optional.of(existingAnswer));

        answerService.softDeleteAnswer(100L);

        assertThat(existingAnswer.getIsDeleted()).isTrue();
        verify(answerRepository).saveAndFlush(existingAnswer);
    }

    @Test
    @DisplayName("softDeleteAnswer - không làm gì khi answer không tồn tại")
    void softDeleteAnswer_DoesNothing_WhenAnswerNotFound() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatCode(() -> answerService.softDeleteAnswer(999L))
                .doesNotThrowAnyException();

        verify(answerRepository, never()).saveAndFlush(any());
    }

    // ===================== reorderAnswers =====================

    @Test
    @DisplayName("reorderAnswers - thành công khi sắp xếp lại thứ tự đáp án")
    void reorderAnswers_Success() {
        Answer answer1 = Answer.builder().answerId(1L).displayOrder(0).isDeleted(false).questionId(10L).build();
        Answer answer2 = Answer.builder().answerId(2L).displayOrder(1).isDeleted(false).questionId(10L).build();
        Answer answer3 = Answer.builder().answerId(3L).displayOrder(2).isDeleted(false).questionId(10L).build();

        when(answerRepository.findByQuestionIdOrderByDisplayOrder(10L))
                .thenReturn(Arrays.asList(answer1, answer2, answer3));

        // Sắp xếp: 3, 1, 2
        answerService.reorderAnswers(10L, Arrays.asList(3L, 1L, 2L));

        assertThat(answer3.getDisplayOrder()).isEqualTo(0);
        assertThat(answer1.getDisplayOrder()).isEqualTo(1);
        assertThat(answer2.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("reorderAnswers - bỏ qua answer đã bị xóa khi sắp xếp")
    void reorderAnswers_SkipsDeletedAnswers() {
        Answer answer1 = Answer.builder().answerId(1L).displayOrder(0).isDeleted(false).questionId(10L).build();
        Answer deletedAnswer = Answer.builder().answerId(2L).displayOrder(1).isDeleted(true).questionId(10L).build();

        when(answerRepository.findByQuestionIdOrderByDisplayOrder(10L))
                .thenReturn(Arrays.asList(answer1, deletedAnswer));

        answerService.reorderAnswers(10L, Arrays.asList(2L, 1L));

        // answer bị xóa không được lưu
        verify(answerRepository, times(1)).save(answer1);
        verify(answerRepository, never()).save(deletedAnswer);
    }
}
