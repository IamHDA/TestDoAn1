package com.vn.backend.unit;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.exam.*;
import com.vn.backend.dto.request.question.QuestionAvailableSearchRequest;
import com.vn.backend.dto.response.exam.ExamResponse;
import com.vn.backend.dto.response.exam.ExamSearchResponse;
import com.vn.backend.dto.response.exam.ExamStatisticResponse;
import com.vn.backend.entities.*;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ExamQuestionRepository;
import com.vn.backend.repositories.ExamRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.ExamServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamServiceImpl Unit Tests")
@TestMethodOrder(MethodOrderer.DisplayName.class)
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

    private ExamServiceImpl examService;

    private User teacherUser;
    private Exam existingExam;
    private Subject existingSubject;

    @BeforeEach
    void setUp() {
        // Thủ công khởi tạo để đạt 100% Method Coverage (bao gồm constructor của class và BaseService)
        examService = new ExamServiceImpl(
                messageUtils,
                authService,
                examRepository,
                subjectRepository,
                questionRepository,
                examQuestionRepository
        );

        teacherUser = User.builder()
                .id(1L)
                .username("teacher01")
                .role(Role.TEACHER)
                .build();

        existingSubject = Subject.builder()
                .subjectId(1L)
                .subjectName("Toán học")
                .subjectCode("MATH101")
                .isDeleted(false)
                .build();

        existingExam = Exam.builder()
                .examId(10L)
                .subjectId(1L)
                .subject(existingSubject) // Quan trọng: Tránh NPE khi ModelMapper map sang SubjectResponse
                .title("Đề thi thử")
                .description("Mô tả đề thi")
                .createdBy(1L)
                .isDeleted(false)
                .build();
    }

    // ===========================================
    // I. Nhóm: Quản lý Đề thi (TC_EXAM_01 - 10)
    // ===========================================

    @Test
    @DisplayName("[TC_EXAM_01] createExam - Thành công")
    void createExam_Success() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setSubjectId("1");
        request.setTitle("Đề thi mới");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(1L)).thenReturn(true);

        examService.createExam(request);

        verify(examRepository).save(any(Exam.class));
    }

    @Test
    @DisplayName("[TC_EXAM_02] createExam - Thất bại do không tìm thấy Môn học")
    void createExam_SubjectNotFound_ThrowsException() {
        ExamCreateRequest request = new ExamCreateRequest();
        request.setSubjectId("99");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(99L)).thenReturn(false);
        when(messageUtils.getMessage(any())).thenReturn("Không tìm thấy môn học");

        assertThatThrownBy(() -> examService.createExam(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_03] searchExam - Thành công với phân trang")
    void searchExam_Success() {
        ExamSearchRequest filter = new ExamSearchRequest();
        BaseFilterSearchRequest<ExamSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(filter);
        
        SearchRequest pr = new SearchRequest();
        pr.setPageNum("1");
        pr.setPageSize("10");
        request.setPagination(pr);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Page<Exam> page = new PageImpl<>(List.of(existingExam));
        when(examRepository.searchExam(any(), any(Pageable.class))).thenReturn(page);

        var response = examService.searchExam(request);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPaging().getTotalRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC_EXAM_04] getExam - Thành công")
    void getExam_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));

        ExamResponse result = examService.getExam("10");

        assertThat(result).isNotNull();
        assertThat(result.getExamId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("[TC_EXAM_05] getExam - Thất bại khi không tìm thấy đề thi")
    void getExam_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.getExam("99"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_06] updateExam - Thành công")
    void updateExam_Success() {
        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setSubjectId("1");
        request.setTitle("Tiêu đề cập nhật");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(1L)).thenReturn(true);

        examService.updateExam("10", request);

        verify(examRepository).save(existingExam);
        assertThat(existingExam.getTitle()).isEqualTo("Tiêu đề cập nhật");
    }

    @Test
    @DisplayName("[TC_EXAM_07] updateExam - Thất bại khi đề thi không tồn tại")
    void updateExam_ExamNotFound_ThrowsException() {
        // Fix: Cung cấp đầy đủ subjectId để tránh NumberFormatException khi toDTO()
        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setSubjectId("1");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.updateExam("99", request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_08] updateExam - Thất bại khi Môn học mới không tồn tại")
    void updateExam_SubjectNotFound_ThrowsException() {
        ExamUpdateRequest request = new ExamUpdateRequest();
        request.setSubjectId("99");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(99L)).thenReturn(false);
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.updateExam("10", request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_09] deleteExam - Thành công")
    void deleteExam_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));

        examService.deleteExam("10");

        assertThat(existingExam.getIsDeleted()).isTrue();
        verify(examRepository).save(existingExam);
    }

    @Test
    @DisplayName("[TC_EXAM_10] deleteExam - Thất bại khi không tìm thấy")
    void deleteExam_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.deleteExam("99"))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // II. Nhóm: Quan hệ Đề thi - Câu hỏi (TC_EXAM_11 - 18)
    // ===========================================

    @Test
    @DisplayName("[TC_EXAM_11] addQuestionsToExam - Hỗn hợp (Cập nhật index và Thêm mới)")
    void addQuestionsToExam_Mixed_Success() {
        // Dữ liệu cũ: Đề thi đang có câu hỏi 1L
        ExamQuestion oldEq = ExamQuestion.builder().examId(10L).questionId(1L).orderIndex(1).build();
        List<ExamQuestion> existing = new ArrayList<>();
        existing.add(oldEq);

        // Request: Cập nhật 1L lên index 2, và Thêm 2L index 3
        ExamQuestionsCreateRequest r1 = new ExamQuestionsCreateRequest();
        r1.setQuestionId("1"); r1.setOrderIndex("2");
        ExamQuestionsCreateRequest r2 = new ExamQuestionsCreateRequest();
        r2.setQuestionId("2"); r2.setOrderIndex("3");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(examQuestionRepository.getAllExamQuestion(10L, 1L)).thenReturn(existing);

        // Mock cho câu hỏi mới 2L
        Question q2 = Question.builder().questionId(2L).build();
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(2L, 1L))
                .thenReturn(Optional.of(q2));

        examService.addQuestionsToExam("10", List.of(r1, r2));

        // Fix: Verification phải là 2 lần vì code gọi saveAll cho toUpdate và saveAll cho toInsert
        verify(examQuestionRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("[TC_EXAM_12] addQuestionsToExam - Thất bại khi đề thi không tồn tại")
    void addQuestionsToExam_ExamNotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.addQuestionsToExam("99", new ArrayList<>()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_13] addQuestionsToExam - Thất bại khi Câu hỏi không tồn tại")
    void addQuestionsToExam_QuestionNotFound_ThrowsException() {
        ExamQuestionsCreateRequest r1 = new ExamQuestionsCreateRequest();
        r1.setQuestionId("99");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(examQuestionRepository.getAllExamQuestion(10L, 1L)).thenReturn(new ArrayList<>());
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.addQuestionsToExam("10", List.of(r1)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_14] removeQuestionsFromExam - Thành công")
    void removeQuestionsFromExam_Success() {
        ExamQuestionsDeleteRequest req = new ExamQuestionsDeleteRequest();
        req.setExamId("10"); req.setQuestionId("1");
        
        ExamQuestion eq = new ExamQuestion();
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(10L, 1L, 1L))
                .thenReturn(Optional.of(eq));

        examService.removeQuestionsFromExam(List.of(req));

        verify(examQuestionRepository).delete(eq);
    }

    @Test
    @DisplayName("[TC_EXAM_15] removeQuestionsFromExam - Thất bại khi không tìm thấy quan hệ")
    void removeQuestionsFromExam_NotFound_ThrowsException() {
        ExamQuestionsDeleteRequest req = new ExamQuestionsDeleteRequest();
        req.setExamId("10"); req.setQuestionId("99");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.removeQuestionsFromExam(List.of(req)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_16] updateQuestionInExam - Thành công")
    void updateQuestionInExam_Success() {
        // Fix: Cung cấp đầy đủ dữ liệu và score (để tránh NPE khi parseDouble)
        ExamQuestionUpdateRequest request = new ExamQuestionUpdateRequest();
        request.setExamId("10"); 
        request.setQuestionId("1"); 
        request.setOrderIndex("5");
        request.setScore("1.5");

        ExamQuestion eq = new ExamQuestion();
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(10L, 1L, 1L))
                .thenReturn(Optional.of(eq));

        examService.updateQuestionInExam(request);

        assertThat(eq.getOrderIndex()).isEqualTo(5);
        assertThat(eq.getScore()).isEqualTo(1.5);
        verify(examQuestionRepository).save(eq);
    }

    @Test
    @DisplayName("[TC_EXAM_17] updateQuestionInExam - Thất bại khi không tìm thấy")
    void updateQuestionInExam_NotFound_ThrowsException() {
        // Fix: Cung cấp đầy đủ ID để toDTO() không lỗi parse
        ExamQuestionUpdateRequest request = new ExamQuestionUpdateRequest();
        request.setExamId("10");
        request.setQuestionId("99");

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.updateQuestionInExam(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_18] searchExamQuestion - Thành công")
    void searchExamQuestion_Success() {
        ExamQuestionsSearchRequest filter = new ExamQuestionsSearchRequest();
        filter.setExamId("10");
        BaseFilterSearchRequest<ExamQuestionsSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(filter);
        
        SearchRequest pr = new SearchRequest();
        pr.setPageNum("1"); pr.setPageSize("10");
        request.setPagination(pr);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Page<ExamQuestion> page = new PageImpl<>(new ArrayList<>());
        when(examQuestionRepository.searchExamQuestion(any(), any(Pageable.class))).thenReturn(page);

        examService.searchExamQuestion(request);

        verify(examQuestionRepository).searchExamQuestion(any(), any());
    }

    // ===========================================
    // III. Nhóm: Vận hành & Thống kê (TC_EXAM_19 - 22)
    // ===========================================

    @Test
    @DisplayName("[TC_EXAM_19] duplicateExam - Thành công")
    void duplicateExam_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        
        ExamQuestion eq = ExamQuestion.builder().examId(10L).questionId(1L).score(1.0).build();
        when(examQuestionRepository.getAllExamQuestion(10L, 1L)).thenReturn(List.of(eq));

        examService.duplicateExam("10");

        verify(examRepository).save(any(Exam.class));
        verify(examQuestionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("[TC_EXAM_20] duplicateExam - Thất bại khi đề gốc không tồn tại")
    void duplicateExam_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(any())).thenReturn("Not Found");

        assertThatThrownBy(() -> examService.duplicateExam("99"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_EXAM_21] getExamStatistic - Thành công")
    void getExamStatistic_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examQuestionRepository.countQuestions(10L, 1L)).thenReturn(10L);
        when(examQuestionRepository.countByDifficulty(10L, 1L)).thenReturn(new ArrayList<>());
        when(examQuestionRepository.countByTopic(10L, 1L)).thenReturn(new ArrayList<>());

        ExamStatisticResponse result = examService.getExamStatistic("10");

        assertThat(result.getTotalQuestion()).isEqualTo(10);
    }

    @Test
    @DisplayName("[TC_EXAM_22] searchAvailableQuestions - Thành công")
    void searchAvailableQuestions_Success() {
        QuestionAvailableSearchRequest filter = new QuestionAvailableSearchRequest();
        filter.setExamId("10");
        filter.setSubjectId("1");
        BaseFilterSearchRequest<QuestionAvailableSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(filter);
        
        SearchRequest pr = new SearchRequest();
        pr.setPageNum("1"); pr.setPageSize("10");
        request.setPagination(pr);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Page<Question> page = new PageImpl<>(new ArrayList<>());
        when(questionRepository.searchAvailableQuestions(any(), any(Pageable.class))).thenReturn(page);
        when(examQuestionRepository.findAllIdsByExamId(anyLong())).thenReturn(Collections.emptySet());

        examService.searchAvailableQuestions(request);

        verify(questionRepository).searchAvailableQuestions(any(), any());
    }

    @Test
    @DisplayName("[TC_EXAM_23] addQuestionsToExam - Chỉ thực hiện xóa câu hỏi")
    void addQuestionsToExam_DeleteOnly_Success() {
        ExamQuestion oldEq = ExamQuestion.builder().examId(10L).questionId(1L).orderIndex(1).build();
        List<ExamQuestion> existing = new ArrayList<>();
        existing.add(oldEq);

        List<ExamQuestionsCreateRequest> emptyRequest = new ArrayList<>();

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(10L, 1L))
                .thenReturn(Optional.of(existingExam));
        when(examQuestionRepository.getAllExamQuestion(10L, 1L)).thenReturn(existing);

        examService.addQuestionsToExam("10", emptyRequest);

        verify(examQuestionRepository).delete(oldEq);
        verify(examQuestionRepository, never()).saveAll(anyList());
    }
}
