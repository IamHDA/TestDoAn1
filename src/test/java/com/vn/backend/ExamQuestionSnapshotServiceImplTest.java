package com.vn.backend;

import com.vn.backend.entities.ExamQuestion;
import com.vn.backend.entities.Question;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.ExamQuestionAnswerSnapshotRepository;
import com.vn.backend.repositories.ExamQuestionRepository;
import com.vn.backend.repositories.ExamQuestionSnapshotRepository;
import com.vn.backend.services.impl.ExamQuestionSnapshotServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamQuestionSnapshotServiceImpl Unit Tests")
class ExamQuestionSnapshotServiceImplTest {

    @Mock
    private ExamQuestionSnapshotRepository examQuestionSnapshotRepository;
    @Mock
    private ExamQuestionAnswerSnapshotRepository examQuestionAnswerSnapshotRepository;
    @Mock
    private ExamQuestionRepository examQuestionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private ExamQuestionSnapshotServiceImpl examQuestionSnapshotService;

    @Test
    @DisplayName("createExamQuestionSnapshots - thành công tạo snapshot từ danh sách câu hỏi")
    void createExamQuestionSnapshots_Success() {
        Long examId = 1L;
        Long sessionExamId = 100L;
        
        Question q = new Question();
        q.setQuestionId(10L);
        q.setContent("What is Java?");
        
        ExamQuestion eq = new ExamQuestion();
        eq.setExamId(examId);
        eq.setQuestion(q);
        eq.setScore(1.0);

        when(examQuestionRepository.getAllExamQuestion(examId)).thenReturn(List.of(eq));

        examQuestionSnapshotService.createExamQuestionSnapshots(examId, sessionExamId);

        verify(examQuestionSnapshotRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("createExamQuestionSnapshots - ném exception khi đề thi trống")
    void createExamQuestionSnapshots_ThrowsException_WhenExamEmpty() {
        Long examId = 1L;
        when(examQuestionRepository.getAllExamQuestion(examId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> examQuestionSnapshotService.createExamQuestionSnapshots(examId, 100L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("getAllQuestions - thành công lấy danh sách snapshot theo thứ tự SEQUENTIAL")
    void getAllQuestions_Success_Sequential() {
        Long sessionExamId = 100L;
        when(examQuestionSnapshotRepository.findAllBySessionExamId(eq(sessionExamId), any()))
                .thenReturn(List.of(new com.vn.backend.entities.ExamQuestionSnapshot()));

        var response = examQuestionSnapshotService.getAllQuestions(sessionExamId, QuestionOrderMode.SEQUENTIAL);

        assertThat(response).isNotEmpty();
        verify(examQuestionSnapshotRepository).findAllBySessionExamId(eq(sessionExamId), any());
    }
}
