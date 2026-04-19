package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.question.*;
import com.vn.backend.dto.request.common.*;
import com.vn.backend.dto.response.common.*;
import com.vn.backend.dto.response.question.*;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.*;
import com.vn.backend.services.impl.QuestionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionServiceImpl Unit Tests - 35 Complete Test Cases")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class QuestionServiceImplTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private ApprovalRequestService approvalRequestService;
    @Mock private AuthService authService;
    @Mock private TopicRepository topicRepository;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private User teacherUser;
    private User adminUser;
    private User studentUser;
    private Topic topic;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).username("teacher").role(Role.TEACHER).build();
        adminUser = User.builder().id(2L).username("admin").role(Role.ADMIN).build();
        studentUser = User.builder().id(3L).username("student").role(Role.STUDENT).build();
        topic = Topic.builder().topicId(10L).topicName("Topic 1").isActive(true).isDeleted(false).build();
    }

    // =========================================================================================
    // I. CREATE QUESTION
    // =========================================================================================

    @Test
    @DisplayName("TC_QLT_01: Tạo câu hỏi thành công (Single Choice)")
    void test_TC_QLT_01_CreateQuestion_Success_SingleChoice() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setType(QuestionType.SINGLE_CHOICE);
        req.setAnswers(List.of(
            createAnsReq("A", true), createAnsReq("B", false)
        ));

        Question savedQ = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.saveAndFlush(any())).thenReturn(savedQ);
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);
        
        QuestionDetailResponse res = questionService.createQuestion(req);
        assertThat(res).isNotNull();
        verify(answerRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("TC_QLT_02: Tạo câu hỏi Multiple Choice hợp lệ")
    void test_TC_QLT_02_CreateQuestion_Success_MultiChoice() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setType(QuestionType.MULTI_CHOICE);
        req.setAnswers(List.of(
            createAnsReq("A", true), createAnsReq("B", true), createAnsReq("C", false)
        ));

        Question savedQ = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.saveAndFlush(any())).thenReturn(savedQ);
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);
        
        assertThatCode(() -> questionService.createQuestion(req)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC_QLT_03: Tạo câu hỏi thất bại do Topic không tồn tại")
    void test_TC_QLT_03_CreateQuestion_Fail_TopicNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(99L);
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class)
            .hasMessage("Not Found");
    }

    @Test
    @DisplayName("TC_QLT_04: Tạo câu hỏi thất bại do Topic bị vô hiệu hóa")
    void test_TC_QLT_04_CreateQuestion_Fail_TopicInactive() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.empty()); // inactive will return empty here
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_05: Tạo câu hỏi thất bại do không có đáp án")
    void test_TC_QLT_05_CreateQuestion_Fail_EmptyAnswers() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setAnswers(new ArrayList<>());
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
    }

    @Test
    @DisplayName("TC_QLT_06: Tạo câu hỏi thất bại do thiếu đáp án ĐÚNG")
    void test_TC_QLT_06_CreateQuestion_Fail_NoCorrectAnswer() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setAnswers(List.of(createAnsReq("A", false), createAnsReq("B", false)));
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
    }

    @Test
    @DisplayName("TC_QLT_07: Bẫy Single Choice có quá 1 đáp án đúng")
    void test_TC_QLT_07_CreateQuestion_Fail_SingleChoiceMultiCorrect() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setType(QuestionType.SINGLE_CHOICE);
        req.setAnswers(List.of(createAnsReq("A", true), createAnsReq("B", true)));
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
    }

    @Test
    @DisplayName("TC_QLT_08: Bẫy Single Choice nhưng có dưới 2 lựa chọn")
    void test_TC_QLT_08_CreateQuestion_Fail_SingleChoiceOnlyOneOption() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setTopicId(10L);
        req.setType(QuestionType.SINGLE_CHOICE);
        req.setAnswers(List.of(createAnsReq("A", true)));
        
        assertThatThrownBy(() -> questionService.createQuestion(req))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
    }

    @Test
    @DisplayName("TC_QLT_09: Tạo hàng loạt thành công")
    void test_TC_QLT_09_BulkCreate_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionBulkCreateItemRequest item = new QuestionBulkCreateItemRequest();
        item.setTopicId(10L);
        item.setType(QuestionType.SINGLE_CHOICE);
        item.setAnswers(List.of(createBulkAnsReq("A", true), createBulkAnsReq("B", false)));

        Question savedQ = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.saveAndFlush(any())).thenReturn(savedQ);
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);
        
        List<QuestionDetailResponse> res = questionService.createQuestions(List.of(item));
        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLT_10: Phá vỡ chuỗi do một phần tử con gặp lỗi")
    void test_TC_QLT_10_BulkCreate_Fail_OneItemMissingAnswers() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));
        
        QuestionBulkCreateItemRequest itemValid = new QuestionBulkCreateItemRequest();
        itemValid.setTopicId(10L);
        itemValid.setType(QuestionType.SINGLE_CHOICE);
        itemValid.setAnswers(List.of(createBulkAnsReq("A", true), createBulkAnsReq("B", false)));

        QuestionBulkCreateItemRequest itemInvalid = new QuestionBulkCreateItemRequest();
        itemInvalid.setTopicId(10L);
        itemInvalid.setAnswers(null);

        assertThatThrownBy(() -> questionService.createQuestions(List.of(itemValid, itemInvalid)))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
    }

    // =========================================================================================
    // II. READ & SEARCH
    // =========================================================================================

    @Test
    @DisplayName("TC_QLT_11: Xem chi tiết câu hỏi thành công")
    void test_TC_QLT_11_GetDetail_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.findByQuestionIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(q));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);
        
        Answer a1 = Answer.builder().answerId(1L).content("A").isDeleted(false).build();
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(new ArrayList<>(List.of(a1)));

        QuestionDetailResponse res = questionService.getQuestionDetail(100L);
        assertThat(res.getId()).isEqualTo(100L);
        assertThat(res.getAnswers()).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLT_12: Xem chi tiết tự động ẩn đáp án đã xóa")
    void test_TC_QLT_12_GetDetail_HidesDeletedAnswers() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).topicId(10L).build();
        when(questionRepository.findByQuestionIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(q));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(10L)).thenReturn(topic);
        
        Answer a1 = Answer.builder().answerId(1L).content("A").isDeleted(false).build();
        Answer a2 = Answer.builder().answerId(2L).content("B").isDeleted(true).build(); // Deleted
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(new ArrayList<>(List.of(a1, a2)));

        QuestionDetailResponse res = questionService.getQuestionDetail(100L);
        assertThat(res.getAnswers()).hasSize(1);
        assertThat(res.getAnswers().get(0).getContent()).isEqualTo("A");
    }

    @Test
    @DisplayName("TC_QLT_13: Ngăn chặn truy cập thẻ đã bị thu hồi")
    void test_TC_QLT_13_GetDetail_Fail_SoftDeleted() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(questionRepository.findByQuestionIdAndIsDeletedFalse(100L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        assertThatThrownBy(() -> questionService.getQuestionDetail(100L))
            .isInstanceOf(AppException.class)
            .hasMessage("Not Found");
    }

    @Test
    @DisplayName("TC_QLT_14: Tìm kiếm quyền Quản trị viên (Admin Layer)")
    void test_TC_QLT_14_Search_Admin() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        BaseFilterSearchRequest<QuestionSearchRequest> req = createSearchReq();
        
        Question q = Question.builder().questionId(1L).topic(topic).build();
        Page<Question> page = new PageImpl<>(List.of(q));
        
        when(questionRepository.searchQuestionsForAdmin(any(), any(), any(), any(), any(), any())).thenReturn(page);
        
        ResponseListData<QuestionSearchResponse> res = questionService.searchQuestions(req);
        assertThat(res.getContent()).hasSize(1);
        verify(questionRepository).searchQuestionsForAdmin(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC_QLT_15: Tìm kiếm quyền Giáo viên (Teacher Layer)")
    void test_TC_QLT_15_Search_Teacher() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        BaseFilterSearchRequest<QuestionSearchRequest> req = createSearchReq();
        
        Question q = Question.builder().questionId(1L).topic(topic).build();
        Page<Question> page = new PageImpl<>(List.of(q));
        
        when(questionRepository.searchQuestions(any(), any(), any(), any(), eq(1L), any(), any())).thenReturn(page);
        
        ResponseListData<QuestionSearchResponse> res = questionService.searchQuestions(req);
        assertThat(res.getContent()).hasSize(1);
        verify(questionRepository).searchQuestions(any(), any(), any(), any(), eq(1L), any(), any());
    }

    @Test
    @DisplayName("TC_QLT_16: Tìm kiếm qua Bộ Lọc Đa Luồng (Filters)")
    void test_TC_QLT_16_Search_Teacher_MultiFilter() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        BaseFilterSearchRequest<QuestionSearchRequest> req = createSearchReq();
        req.getFilters().setType(QuestionType.MULTI_CHOICE);
        req.getFilters().setDifficultyLevel(3);
        
        Page<Question> page = new PageImpl<>(new ArrayList<>());
        when(questionRepository.searchQuestions(
            eq(QuestionType.MULTI_CHOICE), eq(3), any(), any(), eq(1L), any(), any()
        )).thenReturn(page);
        
        questionService.searchQuestions(req);
        
        verify(questionRepository).searchQuestions(
             eq(QuestionType.MULTI_CHOICE), eq(3), any(), any(), eq(1L), any(), any()
        );
    }

    // =========================================================================================
    // III. UPDATE & DELETE
    // =========================================================================================

    @Test
    @DisplayName("TC_QLT_17: Cập nhật thông tin cơ bản thành công")
    void test_TC_QLT_17_Update_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).content("Old").createdBy(1L).build();
        
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(any())).thenReturn(topic);

        QuestionUpdateRequest req = new QuestionUpdateRequest();
        req.setContent("New");
        req.setDifficultyLevel(5);
        
        QuestionDetailResponse res = questionService.updateQuestion(100L, req);
        assertThat(res.getContent()).isEqualTo("New");
        assertThat(res.getDifficultyLevel()).isEqualTo(5);
        verify(questionRepository).saveAndFlush(q);
    }

    @Test
    @DisplayName("TC_QLT_18: Cập nhật linh hoạt một phần (Partial Update)")
    void test_TC_QLT_18_Update_Success_Partial() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).content("Keep Me").createdBy(1L).build();
        
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(any())).thenReturn(topic);

        QuestionUpdateRequest req = new QuestionUpdateRequest();
        req.setImageUrl("new.png"); 
        
        QuestionDetailResponse res = questionService.updateQuestion(100L, req);
        assertThat(res.getContent()).isEqualTo("Keep Me");
        assertThat(res.getImageUrl()).isEqualTo("new.png");
    }

    @Test
    @DisplayName("TC_QLT_19: API không xâm phạm sửa chữa đáp án")
    void test_TC_QLT_19_Update_DoesNotModifyAnswers() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).createdBy(1L).build();
        
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(any())).thenReturn(topic);

        QuestionUpdateRequest req = new QuestionUpdateRequest();
        questionService.updateQuestion(100L, req);
        
        verify(answerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_QLT_20: Tự động gỡ cờ duyệt 'Bài Ôn Tập'")
    void test_TC_QLT_20_Update_ResetsReviewStatus() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).createdBy(1L).isReviewQuestion(true).build(); // TRUE
        
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.of(q));
        when(questionRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(any())).thenReturn(topic);

        QuestionUpdateRequest req = new QuestionUpdateRequest();
        req.setContent("Change");
        questionService.updateQuestion(100L, req);
        
        assertThat(q.getIsReviewQuestion()).isFalse(); // RESET TO FALSE
    }

    @Test
    @DisplayName("TC_QLT_21: Bẫy Bảo mật - Người dùng sai Role thao tác")
    void test_TC_QLT_21_Update_Fail_NotTeacher() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        
        assertThatThrownBy(() -> questionService.updateQuestion(100L, new QuestionUpdateRequest()))
            .isInstanceOf(AppException.class)
            .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.UNAUTHORIZED));
    }

    @Test
    @DisplayName("TC_QLT_22: Bẫy IDOR - Sửa chéo câu hỏi")
    void test_TC_QLT_22_Update_Fail_NotOwner() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        // Returns empty meaning the question doesn't belong to them or is deleted
        when(questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(100L, 1L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        assertThatThrownBy(() -> questionService.updateQuestion(100L, new QuestionUpdateRequest()))
            .isInstanceOf(AppException.class)
            .hasMessage("Not Found");
    }

    @Test
    @DisplayName("TC_QLT_23: Xóa mềm (Soft Delete) thành công")
    void test_TC_QLT_23_SoftDelete_Success() {
        Question q = Question.builder().questionId(100L).isDeleted(false).build();
        when(questionRepository.findById(100L)).thenReturn(Optional.of(q));
        
        questionService.softDeleteQuestion(100L);
        assertThat(q.getIsDeleted()).isTrue();
        verify(questionRepository).save(q);
    }

    // =========================================================================================
    // IV. IMPORT/EXPORT EXCEL
    // =========================================================================================

    private MockMultipartFile generateMockExcel() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            Row row = sheet.createRow(1); // Skip header row 0
            row.createCell(1).setCellValue(10L); // topicId
            row.createCell(2).setCellValue(1); // Difficulty
            row.createCell(3).setCellValue("Hỏi gì?"); // Question
            row.createCell(4).setCellValue("A");
            row.createCell(5).setCellValue("B");
            row.createCell(6).setCellValue("C");
            row.createCell(7).setCellValue("D");
            row.createCell(8).setCellValue(2); // Correct answer is B

            wb.write(out);
            return new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    @DisplayName("TC_Q_24: Đọc luồng File Excel thuần thành công")
    void test_TC_Q_24_ImportExcel_Success() throws IOException {
        MockMultipartFile file = generateMockExcel();
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

        QuestionBulkCreateRequest res = questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest());
        assertThat(res.getQuestions()).hasSize(1);
        assertThat(res.getQuestions().get(0).getContent()).isEqualTo("Hỏi gì?");
        assertThat(res.getQuestions().get(0).getAnswers().get(1).getIsCorrect()).isTrue();
    }

    @Test
    @DisplayName("TC_Q_25: Tự động chuyển Sheet dữ liệu khi tên Sheet lỗi")
    void test_TC_Q_25_ImportExcel_FallbackSheet() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("Wrong Name"); // Sheet 0
            Sheet sheet = wb.createSheet("Not Import"); // Sheet 1
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue(10L);
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Hỏi gì?");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue(1); // Correct answer 1

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", null, out.toByteArray());
            
            when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

            QuestionBulkCreateRequest res = questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest());
            assertThat(res.getQuestions()).hasSize(1);
        }
    }

    @Test
    @DisplayName("TC_Q_26: Hệ thống phớt lờ các hàng Lưới Trống (Empty Rows)")
    void test_TC_Q_26_ImportExcel_SkipsEmptyRows() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            sheet.createRow(1); // Empty row
            
            Row row = sheet.createRow(2);
            row.createCell(1).setCellValue(10L);
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Q");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue(1); 

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test", null, out.toByteArray());
            when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

            QuestionBulkCreateRequest res = questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest());
            assertThat(res.getQuestions()).hasSize(1); // row 1 skipped
        }
    }

    @Test
    @DisplayName("TC_Q_27: Hệ thống bỏ qua (Skips) hàng bị gõ sai Chủ Đề")
    void test_TC_Q_27_ImportExcel_SkipsInvalidTopic() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue(99L); // Invalid Topic
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Q");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue(1); 

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test", null, out.toByteArray());
            when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

            QuestionBulkCreateRequest res = questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest());
            assertThat(res.getQuestions()).hasSize(0); // SKIPPED
        }
    }

    @Test
    @DisplayName("TC_Q_28: Trình Ép kiểu mềm (Number/String Parsing) cho Level")
    void test_TC_Q_28_ImportExcel_ParseLevelAsNumberOrString() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue("10"); // String
            row.createCell(2).setCellValue("5"); // String
            row.createCell(3).setCellValue("Q");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue("1"); // String

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test", null, out.toByteArray());
            when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

            QuestionBulkCreateRequest res = questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest());
            assertThat(res.getQuestions()).hasSize(1); 
            assertThat(res.getQuestions().get(0).getDifficultyLevel()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("TC_Q_29: Bắt lỗi Đáp Án trỏ vào vùng khí khuyết")
    void test_TC_Q_29_ImportExcel_Fail_CorrectAnswerEmpty() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue(10);
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Q");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue(2); // Says index 2 but Answer 2 is blank!

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test", null, out.toByteArray());
            when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(Optional.of(topic));

            assertThatThrownBy(() -> questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest()))
                .isInstanceOf(AppException.class)
                .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
        }
    }

    @Test
    @DisplayName("TC_Q_30: Bắt lỗi Số Index vượt biên đáp án")
    void test_TC_Q_30_ImportExcel_Fail_CorrectAnswerIndexOutOfBounds() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Import");
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue(10);
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue("Q");
            row.createCell(4).setCellValue("A");
            row.createCell(8).setCellValue(5); // 5 is > 4

            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "test", null, out.toByteArray());

            assertThatThrownBy(() -> questionService.importQuestionsFromExcel(file, new QuestionImportExcelRequest()))
                .isInstanceOf(AppException.class)
                .matches(ex -> ((AppException) ex).getCode().equals(AppConst.MessageConst.INVALID_LOGIC_QUESTION));
        }
    }

    @Test
    @DisplayName("TC_QLT_31: Export xuất chính xác số đo cột dư")
    void test_TC_QLT_31_ExportExcel_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        Question q = Question.builder().questionId(100L).content("Q1").topic(topic).build();
        Page<Question> page = new PageImpl<>(List.of(q));
        when(questionRepository.searchQuestions(any(), any(), any(), any(), eq(1L), any(), any())).thenReturn(page);
        
        Answer a1 = Answer.builder().content("A").build();
        Answer a2 = Answer.builder().content("B").build();
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(new ArrayList<>(List.of(a1, a2))); // max answers 2
        
        byte[] excelBytes = questionService.exportQuestionsToExcel(new QuestionSearchRequest());
        assertThat(excelBytes).isNotEmpty();
    }

    @Test
    @DisplayName("TC_QLT_32: Generate File Template khảm List Chủ Đề Động")
    void test_TC_QLT_32_DownloadTemplate_Success() {
        List<Object[]> topicsObj = new ArrayList<>();
        topicsObj.add(new Object[]{10L, "Topic1", "Subj1", "SUBJ01"});
        when(topicRepository.listTopicsWithSubject()).thenReturn(topicsObj);
        
        byte[] bytes = questionService.downloadImportTemplate();
        assertThat(bytes).isNotEmpty();
    }

    // =========================================================================================
    // V. APPROVAL REQUESTS
    // =========================================================================================

    @Test
    @DisplayName("TC_QLT_33: Nhóm gửi thông điệp kiểm duyệt thành công")
    void test_TC_QLT_33_CreateApprovalQuestion_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).createdBy(1L).isDeleted(false).build();
        when(questionRepository.findById(100L)).thenReturn(Optional.of(q));

        CreateApprovalQuestionRequest req = new CreateApprovalQuestionRequest();
        req.setQuestionIds(List.of(100L));
        req.setRequestType(RequestType.QUESTION_REVIEW_CREATE);

        questionService.createApprovalQuestion(req);
        verify(approvalRequestService).createRequest(any(), any(), eq(1L), any());
    }

    @Test
    @DisplayName("TC_QLT_34: Ngăn chặn gửi đi câu hỏi Tàn Tích (Deleted)")
    void test_TC_QLT_34_CreateApprovalQuestion_Fail_Deleted() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Question q = Question.builder().questionId(100L).createdBy(1L).isDeleted(true).build(); // Deleted
        when(questionRepository.findById(100L)).thenReturn(Optional.of(q));

        CreateApprovalQuestionRequest req = new CreateApprovalQuestionRequest();
        req.setQuestionIds(List.of(100L));
        
        assertThatThrownBy(() -> questionService.createApprovalQuestion(req))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Question not found or deleted");
    }

    @Test
    @DisplayName("TC_QLT_35: Ngăn chặn cướp luồng sở hữu (Cross-teacher IDOR)")
    void test_TC_QLT_35_CreateApprovalQuestion_Fail_IDOR() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID = 1
        Question q = Question.builder().questionId(100L).createdBy(99L).isDeleted(false).build(); // Belongs to user 99
        when(questionRepository.findById(100L)).thenReturn(Optional.of(q));

        CreateApprovalQuestionRequest req = new CreateApprovalQuestionRequest();
        req.setQuestionIds(List.of(100L));
        
        assertThatThrownBy(() -> questionService.createApprovalQuestion(req))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("You can only create approval request for your own questions");
    }

    // ===================== HELPER METHODS =====================
    private AnswerCreateRequest createAnsReq(String content, boolean correct) {
        AnswerCreateRequest r = new AnswerCreateRequest();
        r.setContent(content);
        r.setIsCorrect(correct);
        return r;
    }
    
    private QuestionBulkAnswerCreateRequest createBulkAnsReq(String content, boolean correct) {
        QuestionBulkAnswerCreateRequest r = new QuestionBulkAnswerCreateRequest();
        r.setContent(content);
        r.setIsCorrect(correct);
        return r;
    }

    private BaseFilterSearchRequest<QuestionSearchRequest> createSearchReq() {
        BaseFilterSearchRequest<QuestionSearchRequest> req = new BaseFilterSearchRequest<>();
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        req.setPagination(pagination);
        req.setFilters(new QuestionSearchRequest());
        return req;
    }
}
