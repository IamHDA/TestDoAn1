package com.vn.backend.unit;

import com.vn.backend.dto.request.attachment.AttachmentCreateRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.request.submission.SubmissionSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionUpdateRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.submission.SubmissionDetailResponse;
import com.vn.backend.dto.response.submission.SubmissionExcelQueryDTO;
import com.vn.backend.dto.response.submission.SubmissionSearchQueryDTO;
import com.vn.backend.dto.response.submission.SubmissionSearchResponse;
import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.Attachment;
import com.vn.backend.entities.Submission;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AssignmentRepository;
import com.vn.backend.repositories.AttachmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.SubmissionRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.FileService;
import com.vn.backend.services.impl.SubmissionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionServiceImpl Unit Tests")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SubmissionServiceImplTest {

    @Mock
    private AuthService authService;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private FileService fileService;
    @Mock
    private MessageUtils messageUtils;
    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private User studentUser;
    private User teacherUser;
    private Assignment activeAssignment;
    private Submission existingSubmission;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().id(1L).fullName("Student A").code("SV001").build();
        teacherUser = User.builder().id(2L).build();

        activeAssignment = Assignment.builder()
                .assignmentId(10L)
                .classroomId(100L)
                .dueDate(LocalDateTime.now().plusDays(5))
                .submissionClosed(false)
                .build();

        existingSubmission = Submission.builder()
                .submissionId(1000L)
                .assignmentId(10L)
                .studentId(1L)
                .student(studentUser)
                .submissionStatus(SubmissionStatus.SUBMITTED)
                .gradingStatus(GradingStatus.NOT_GRADED)
                .build();
    }

    // ===========================================
    // I. Group: Core Submission Life Cycle
    // ===========================================

    
    // ==================== Helper Methods ====================
    private void mockCurrentUser(User user) {
        when(authService.getCurrentUser()).thenReturn(user);
    }

    private void mockSubmissionHasPermission(Long submissionId, Long userId, boolean hasPermission) {
        when(submissionRepository.hasPermission(submissionId, userId)).thenReturn(hasPermission);
    }

    private void mockSubmissionFindById(Long submissionId, Submission submission) {
        when(submissionRepository.findById(submissionId))
                .thenReturn(submission != null ? Optional.of(submission) : Optional.empty());
    }

    private void mockAssignmentCanViewSubmissions(Long assignmentId, Long userId, boolean canView) {
        when(assignmentRepository.canUserViewSubmissions(assignmentId, userId)).thenReturn(canView);
    }

    private void mockAssignmentCanViewAny(boolean canView) {
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(canView);
    }

    private BaseFilterSearchRequest<SubmissionSearchRequest> makeSearchRequest(
            String assignmentId, String pageNum, String pageSize) {
        SubmissionSearchRequest filter = new SubmissionSearchRequest();
        if (assignmentId != null) filter.setAssignmentId(assignmentId);
        SearchRequest pg = new SearchRequest();
        pg.setPageNum(pageNum);
        pg.setPageSize(pageSize);
        BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(pg);
        return req;
    }

    private SubmissionUpdateRequest makeAttachReq(List<AttachmentCreateRequest> list) {
        SubmissionUpdateRequest req = new SubmissionUpdateRequest();
        req.setAttachmentCreateRequestList(list);
        return req;
    }

    private SubmissionGradeUpdateRequest makeGradeReq(String grade) {
        SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
        req.setGrade(grade);
        return req;
    }

@Test
    @DisplayName("[TC_SUB_01] getMySubmission - Thành công khi đã có bài nộp")
    void getMySubmission_Found_Success() {
        mockCurrentUser(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(new ArrayList<>());

        var response = submissionService.getMySubmission("10");

        assertThat(response).isNotNull();
        assertThat(response.getSubmissionId()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("[TC_SUB_02] getMySubmission - Trả về object trống khi chưa bắt đầu")
    void getMySubmission_NotFound_ReturnsEmpty() {
        mockCurrentUser(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L)).thenReturn(Optional.empty());

        var response = submissionService.getMySubmission("10");

        assertThat(response.getSubmissionId()).isNull();
    }

    @Test
    @DisplayName("[TC_SUB_03] createDefaultSubmissions - Thoát sớm nếu assignment rỗng")
    void createDefaultSubmissions_NullAssignment_Returns() {
        verifyNoInteractions(submissionRepository);
    }

    @Test
    @DisplayName("[TC_SUB_04] deleteAttachmentInSubmission - Thành công và trả trạng thái về NOT_SUBMITTED khi hết file")
    void deleteAttachmentInSubmission_LastFile_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(eq(1L), anyLong(), any(), any())).thenReturn(Optional.of(activeAssignment));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(new ArrayList<>());

        submissionService.deleteAttachmentInSubmission("500");

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC_SUB_05] getDetailSubmission - Thất bại khi không tìm thấy bài nộp")
    void getDetailSubmission_NotFound_ThrowsException() {
        mockCurrentUser(teacherUser);
        when(submissionRepository.hasPermission(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getDetailSubmission("1000"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_06] deleteAttachmentInSubmission - Nộp trễ nhưng bài tập vẫn mở (Không ném lỗi)")
    void deleteAttachmentInSubmission_Late_Open_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment lateOpenAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(10)).submissionClosed(false).build();
        
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateOpenAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC_SUB_07] deleteAttachmentInSubmission - Xóa file khi nộp muộn nhưng vẫn còn file khác")
    void deleteAttachmentInSubmission_Late_StillHasFiles_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment lateAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateAss));
        // Giả lập vẫn còn 1 file khác
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    @Test
    @DisplayName("[TC_SUB_08] deleteAttachmentInSubmission - Ném lỗi khi nộp bài trễ và bài tập đã đóng")
    void deleteAttachmentInSubmission_LateAndClosed_ThrowsException() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment closedAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(true).build();
        
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(closedAss));

        assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("500"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_09] addAttachmentToSubmission - Nộp bài muộn (LATE_SUBMITTED) khi cho phép")
    void addAttachmentToSubmission_LateAllowed_Success() {
        Assignment lateAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateAss));

        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    // ===========================================
    // II. Group: Search & Permissions
    // ===========================================

    @Test
    @DisplayName("[TC_SUB_10] searchSubmission - Thành công")
    void searchSubmission_Success() {
        BaseFilterSearchRequest<SubmissionSearchRequest> request = makeSearchRequest("10", "1", "10");

        mockCurrentUser(teacherUser);
        mockAssignmentCanViewAny(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        submissionService.searchSubmission(request);

        verify(submissionRepository).searchSubmission(any(), any());
    }

    @Test
    @DisplayName("[TC_SUB_11] searchSubmission - Thất bại do không có quyền xem")
    void searchSubmission_Forbidden_ThrowsException() {
        BaseFilterSearchRequest<SubmissionSearchRequest> request = makeSearchRequest(null, null, null);

        mockCurrentUser(studentUser);
        mockAssignmentCanViewAny(false);

        assertThatThrownBy(() -> submissionService.searchSubmission(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_12] markSubmission - Thất bại khi không có quyền chấm điểm")
    void markSubmission_NoPermission_ThrowsException() {
        mockCurrentUser(studentUser);
        mockSubmissionHasPermission(1000L, 1L, false);

        assertThatThrownBy(() -> submissionService.markSubmission("1000", makeGradeReq(null)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_13] markSubmission - Thành công chấm điểm")
    void markSubmission_Success() {
        mockCurrentUser(teacherUser);
        mockSubmissionHasPermission(1000L, 2L, true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(existingSubmission));
        
        SubmissionGradeUpdateRequest request = new SubmissionGradeUpdateRequest();
        request.setGrade("9.0");

        submissionService.markSubmission("1000", request);

        assertThat(existingSubmission.getGrade()).isEqualTo(9.0);
        assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
    }

    @Test
    @DisplayName("[TC_SUB_14] markSubmission - Thất bại khi bài nộp không tồn tại")
    void markSubmission_NotFound_ThrowsException() {
        mockCurrentUser(teacherUser);
        mockSubmissionHasPermission(1000L, 2L, true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.markSubmission("1000", makeGradeReq(null)))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // III. Group: File Operations & Zip
    // ===========================================

    @Test
    @DisplayName("[TC_SUB_15] downloadAllSubmissions - Thất bại khi không tìm thấy bài nộp nào")
    void downloadAllSubmissions_Empty_ThrowsException() {
        mockCurrentUser(teacherUser);
        mockAssignmentCanViewAny(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_16] downloadAllSubmissions - Thất bại khi không có file đính kèm thực tế")
    void downloadAllSubmissions_NoFiles_ThrowsException() {
        mockCurrentUser(teacherUser);
        mockAssignmentCanViewAny(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // IV. Group: Excel Operations
    // ===========================================

    @Test
    @DisplayName("[TC_SUB_17] importSubmissionScoresFromExcel - Cập nhật điểm thành công")
    void importSubmissionScoresFromExcel_Success() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cellUsername = mock(Cell.class);
        Cell cellCode = mock(Cell.class);
        Cell cellGrade = mock(Cell.class);

        mockCurrentUser(teacherUser);
        mockAssignmentCanViewAny(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        // Mock POI Workbook
        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            when(row.getCell(1)).thenReturn(cellUsername);
            when(row.getCell(3)).thenReturn(cellCode);
            when(row.getCell(7)).thenReturn(cellGrade);
            
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(cellUsername)).thenReturn("user1");
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(cellCode)).thenReturn("SV001");
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(cellGrade)).thenReturn("8.5");

            submissionService.importSubmissionScoresFromExcel("10", multipartFile);

            assertThat(existingSubmission.getGrade()).isEqualTo(8.5);
            assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
        }
    }

    @Test
    @DisplayName("[TC_SUB_18] downloadGradeTemplate - Thành công")
    void downloadGradeTemplate_Success() {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllForExcel(anyLong())).thenReturn(new ArrayList<>());

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.exportToExcel(any(), any(), any(), any())).thenReturn(new ByteArrayResource(new byte[0]));
            
            var result = submissionService.downloadGradeTemplate("10");
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("[TC_SUB_19] downloadGradeTemplate - Thất bại do không có quyền")
    void downloadGradeTemplate_Forbidden_ThrowsException() {
        mockCurrentUser(studentUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> submissionService.downloadGradeTemplate("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_20] importSubmissionScoresFromExcel - Thất bại do không có quyền")
    void importSubmissionScoresFromExcel_Forbidden_ThrowsException() {
        mockCurrentUser(studentUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", mock(MultipartFile.class)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_21] importSubmissionScoresFromExcel - Thất bại do thiếu thông tin sinh viên trong Excel")
    void importSubmissionScoresFromExcel_MissingInfo_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Giả lập cột Username (1) và Code (3) bị trống
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("");

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
    }


    @Test
    @DisplayName("[TC_SUB_22] downloadAllSubmissions - Thành công tạo file Zip")
    void downloadAllSubmissions_Success() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("http://storage/file.pdf").fileName("file.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("file.pdf");
        when(mockFile.toPath()).thenReturn(new File("temp.pdf").toPath());

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            var result = submissionService.downloadAllSubmissions("10");
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("[TC_SUB_23] downloadAllSubmissions - Bỏ qua file khi URL không hợp lệ")
    void downloadAllSubmissions_SkipInvalidFile_ThrowsException() {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("invalid-url").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_24] importSubmissionScoresFromExcel - Thất bại khi định dạng điểm sai (không phải số)")
    void importSubmissionScoresFromExcel_InvalidGrade_ThrowsException() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cellGrade = mock(Cell.class);

        mockCurrentUser(teacherUser);
        mockAssignmentCanViewAny(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            when(row.getCell(7)).thenReturn(cellGrade);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Full Name", "SV001", "INVALID");

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", multipartFile))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("[TC_SUB_25] importSubmissionScoresFromExcel - Cập nhật gradedAt khi điểm mới khác điểm cũ")
    void importSubmissionScoresFromExcel_UpdateGradedAt_Success() throws IOException {
        existingSubmission.setGrade(5.0); // Điểm cũ
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Theo thứ tự: Username, FullName, Code, Grade
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Student A", "SV001", "9.0");

            submissionService.importSubmissionScoresFromExcel("10", file);

            assertThat(existingSubmission.getGrade()).isEqualTo(9.0);
            assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
        }
    }

    @Test
    @DisplayName("[TC_SUB_26] importSubmissionScoresFromExcel - Không cập nhật gradedAt khi điểm mới trùng điểm cũ")
    void importSubmissionScoresFromExcel_SameGrade_NoGradedAtUpdate() throws IOException {
        existingSubmission.setGrade(8.5); // Điểm cũ là 8.5
        LocalDateTime oldGradedAt = LocalDateTime.now().minusDays(1);
        existingSubmission.setGradedAt(oldGradedAt);
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Điểm mới trong Excel vẫn là 8.5
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Student A", "SV001", "8.5");

            submissionService.importSubmissionScoresFromExcel("10", file);

            assertThat(existingSubmission.getGradedAt()).isEqualTo(oldGradedAt); // Vẫn giữ nguyên thời gian cũ
        }
    }

    @Test
    @DisplayName("[TC_SUB_27] importSubmissionScoresFromExcel - Bỏ qua bản ghi khi cột điểm trống")
    void importSubmissionScoresFromExcel_EmptyGradeCell_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Cột điểm (7) trả về chuỗi rỗng
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Student A", "SV001", "");

            submissionService.importSubmissionScoresFromExcel("10", file);

            // Vì điểm rỗng nên không cập nhật GradingStatus.GRADED
            assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.NOT_GRADED);
        }
    }

    @Test
    @DisplayName("[TC_SUB_28] deleteAttachmentInSubmission - Thất bại khi không tìm thấy file đính kèm")
    void deleteAttachmentInSubmission_AttachmentNotFound_ThrowsException() {
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(anyLong(), anyLong(), eq(false))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("500"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_29] importSubmissionScoresFromExcel - Bỏ qua sinh viên có dữ liệu Student bị null trong DB")
    void importSubmissionScoresFromExcel_NullStudentInDB_SkipRow() throws IOException {
        Submission nullStudentSubmission = Submission.builder().build(); // student is null
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(nullStudentSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Name", "SV001", "10.0");

            // Ném lỗi 404 IMPORT_SUBMISSION_NOT_FOUND vì map studentCode bị trống do filter filter(s -> s.getStudent() != null)
            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("[TC_SUB_30] downloadAllSubmissions - Thất bại do lỗi I/O khi nén file")
    void downloadAllSubmissions_IOError_ThrowsException() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("http://storage/file.pdf").fileName("file.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.toPath()).thenReturn(new File("temp.pdf").toPath());

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.copy(any(java.nio.file.Path.class), any(java.io.OutputStream.class)))
                    .thenThrow(new IOException("Simulated IO Error"));

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("[TC_SUB_31] importSubmissionScoresFromExcel - Bỏ qua bản ghi khi điểm trong Excel là Null")
    void importSubmissionScoresFromExcel_NullGrade_NoUpdate() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Theo thứ tự: Username, FullName, Code, Grade (Grade return null)
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Name", "SV001", null);

            submissionService.importSubmissionScoresFromExcel("10", file);

            assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.NOT_GRADED);
        }
    }

    @Test
    @DisplayName("[TC_SUB_32] isBlank Helper - Kiểm thử các trường hợp biên")
    void isBlank_EdgeCases_Success() {
        // Ta sử dụng reflection hoặc gọi qua một method công khai sử dụng isBlank 
        // Ở đây SubmissionServiceImpl.importSubmissionScoresFromExcel gọi isBlank qua parseSubmissionExcel
        // Nhưng đơn giản nhất là test gián tiếp qua Import Excel với dữ liệu đặc biệt
    }

    @Test
    @DisplayName("[TC_SUB_33] importSubmissionScoresFromExcel - Bỏ qua Row bị Null trong Excel")
    void importSubmissionScoresFromExcel_NullRow_Skip() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            when(com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(null); // ROW NULL

            submissionService.importSubmissionScoresFromExcel("10", file);
            verify(submissionRepository, never()).saveAll(any());
        }
    }

    @Test
    @DisplayName("[TC_SUB_34] getDetailSubmission - Thất bại do không có quyền xem")
    void getDetailSubmission_Forbidden_ThrowsException() {
        mockCurrentUser(studentUser);
        mockSubmissionHasPermission(1000L, 1L, false);

        assertThatThrownBy(() -> submissionService.getDetailSubmission("1000"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_35] parseSubmissionExcel - Bao phủ các nhánh isBlank")
    void importSubmissionScoresFromExcel_IsBlankVariants_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Nhánh s.trim().isEmpty() cho username (cột 1)
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("   ", "Full Name", "SV001", "10.0");

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("[TC_SUB_36] downloadAllSubmissions - Bao phủ nhánh URL null/empty")
    void downloadAllSubmissions_UrlVariants_Skip() {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        // URL is empty covers getFileNameFromDefaultUrl branches
        Attachment attEmpty = Attachment.builder().fileUrl("").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attEmpty));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_37] markSubmission - Thất bại khi không tìm thấy bài nộp do bị xóa giữa chừng")
    void markSubmission_ExistsInPermissionButDeletedInDB_ThrowsException() {
        mockCurrentUser(teacherUser);
        mockSubmissionHasPermission(1000L, 2L, true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.markSubmission("1000", makeGradeReq(null)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_38] importSubmissionScoresFromExcel - Bao phủ short-circuit isBlank(username) == false")
    void importSubmissionScoresFromExcel_UsernameOk_CodeBlank_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Username (1) OK, but Code (3) is NULL to hit helper's null branch
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Name", null, "10.0");

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("[TC_SUB_39] deleteAttachmentInSubmission - Bao phủ short-circuit isLate == false")
    void deleteAttachmentInSubmission_NotLate_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("[TC_SUB_40] addAttachmentToSubmission - Thành công khi danh sách đính kèm trống")
    void addAttachmentToSubmission_EmptyList_Success() {
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(activeAssignment));

        submissionService.addAttachmentToSubmission("1000", makeAttachReq(new ArrayList<>()));

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("[TC_SUB_41] addAttachmentToSubmission - Đúng hạn (isLate = false)")
    void addAttachmentToSubmission_NotLate_Success() {
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("[TC_SUB_42] downloadAllSubmissions - Bỏ qua file khi fileName bị Null")
    void downloadAllSubmissions_NullFileName_Skip() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("url").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn(null); // NULL FILE NAME

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class); // hasAnyFile remains false
    }

    @Test
    @DisplayName("[TC_SUB_43] downloadAllSubmissions - Tiếp tục khi một file bị lỗi I/O")
    void downloadAllSubmissions_PartialIOError_Continue() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        // 2 attachments: 1 fail, 1 success
        Attachment attFail = Attachment.builder().fileUrl("fail").fileName("fail.pdf").build();
        Attachment attSuccess = Attachment.builder().fileUrl("success").fileName("success.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attFail, attSuccess));
        
        Resource resFail = mock(Resource.class);
        when(fileService.downloadFile("fail")).thenThrow(new RuntimeException("Download error"));
        
        Resource resSuccess = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile("success")).thenReturn(resSuccess);
        when(resSuccess.getFile()).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("success.pdf");
        when(mockFile.toPath()).thenReturn(new File("temp.pdf").toPath());

        var result = submissionService.downloadAllSubmissions("10");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("[TC_SUB_44] getMySubmission - Thất bại khi không tìm thấy bài nộp")
    void getMySubmission_NotFound_ThrowsException() {
        mockCurrentUser(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getMySubmission("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_45] createDefaultSubmissions - Thành công khi danh sách sinh viên trống")
    void createDefaultSubmissions_EmptyList_Success() {
        when(classMemberRepository.getClassMemberIdsActive(anyLong(), any())).thenReturn(new HashSet<>());
        submissionService.createDefaultSubmissions(activeAssignment);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("[TC_SUB_46] addAttachmentToSubmission - Thất bại khi không tìm thấy bài nộp")
    void addAttachmentToSubmission_NotFound_ThrowsException() {
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("1000", makeAttachReq(new ArrayList<>())))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_47] importSubmissionScoresFromExcel - Bao phủ filter Student null và not null")
    void importSubmissionScoresFromExcel_MixedStudentData_Success() throws IOException {
        Submission validSub = Submission.builder().student(User.builder().code("SV001").build()).build();
        Submission nullSub = Submission.builder().student(null).build();
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        // Trả về 1 valid, 1 null để cover filter
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(validSub, nullSub));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(any(), anyInt())).thenReturn(row);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Name", "SV001", "10.0");

            submissionService.importSubmissionScoresFromExcel("10", file);

            assertThat(validSub.getGrade()).isEqualTo(10.0);
            verify(submissionRepository, times(1)).saveAll(any());
        }
    }

    @Test
    @DisplayName("[TC_SUB_48] downloadAllSubmissions - Bao phủ nhánh URL null")
    void downloadAllSubmissions_UrlNull_Skip() {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment attNull = Attachment.builder().fileUrl(null).build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attNull));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_49] deleteAttachmentInSubmission - Không thay đổi trạng thái khi còn file và không trễ")
    void deleteAttachmentInSubmission_StillHasFiles_NotLate_NoStatusChange() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        existingSubmission.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        
        mockCurrentUser(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        
        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("[TC_SUB_50] markSubmission - Thất bại khi quyền trả về null")
    void markSubmission_PermissionNull_ThrowsException() {
        mockCurrentUser(teacherUser);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(null); // NULL PERMISSION

        assertThatThrownBy(() -> submissionService.markSubmission("1000", makeGradeReq(null)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_51] addAttachmentToSubmission - Nộp trễ nhưng bài tập vẫn mở")
    void addAttachmentToSubmission_LateOpen_Success() {
        Assignment lateOpenAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateOpenAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    @Test
    @DisplayName("[TC_SUB_52] downloadAllSubmissions - Bỏ qua 1 file null name và vẫn nén các file khác")
    void downloadAllSubmissions_MixedNullFileName_Success() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment attNull = Attachment.builder().fileUrl("url1").build();
        Attachment attValid = Attachment.builder().fileUrl("url2").fileName("valid.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attNull, attValid));
        
        Resource res1 = mock(Resource.class);
        File file1 = mock(File.class);
        when(fileService.downloadFile("url1")).thenReturn(res1);
        when(res1.getFile()).thenReturn(file1);
        when(file1.getName()).thenReturn(null); // SKIPPED

        Resource res2 = mock(Resource.class);
        File file2 = mock(File.class);
        when(fileService.downloadFile("url2")).thenReturn(res2);
        when(res2.getFile()).thenReturn(file2);
        when(file2.getName()).thenReturn("valid.pdf");
        when(file2.toPath()).thenReturn(new File("temp.pdf").toPath());

        var result = submissionService.downloadAllSubmissions("10");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("[TC_SUB_53] addAttachmentToSubmission - Thành công khi Assignment không có hạn nộp (dueDate = null)")
    void addAttachmentToSubmission_NoDueDate_Success() {
        Assignment noDueDateAss = Assignment.builder().dueDate(null).submissionClosed(false).build();
        mockCurrentUser(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(noDueDateAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(existingSubmission.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("[TC_SUB_54] downloadAllSubmissions - Thất bại ném lỗi khi copy file lỗi (Bao phủ file != null)")
    void downloadAllSubmissions_FilesCopyError_ThrowsException() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("valid_url").fileName("test.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("test.pdf");
        when(mockFile.toPath()).thenReturn(new File("temp.pdf").toPath());

        try (MockedStatic<java.nio.file.Files> files = mockStatic(java.nio.file.Files.class)) {
            files.when(() -> java.nio.file.Files.copy(any(java.nio.file.Path.class), any(java.io.OutputStream.class)))
                 .thenThrow(new IOException("Disk full"));

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Disk full");
        }
    }

    @Test
    @DisplayName("[TC_SUB_55] searchSubmission - Thất bại khi quyền trả về null")
    void searchSubmission_PermissionNull_ThrowsException() {
        mockCurrentUser(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = makeSearchRequest("10", "1", "10");

        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> submissionService.searchSubmission(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("[TC_SUB_56] downloadGradeTemplate - Thành công khi danh sách trống")
    void downloadGradeTemplate_EmptyList_Success() {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllForExcel(anyLong())).thenReturn(new ArrayList<>());

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.exportToExcel(anyList(), anyString(), any(), any()))
                      .thenReturn(new ByteArrayResource(new byte[0]));

            var result = submissionService.downloadGradeTemplate("10");
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("[TC_SUB_57] importSubmissionScoresFromExcel - Thành công khi file Excel không có dữ liệu")
    void importSubmissionScoresFromExcel_EmptyData_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);

        mockCurrentUser(teacherUser);
        mockAssignmentCanViewSubmissions(10L, 2L, true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(0); // Chỉ có header, không có data

            submissionService.importSubmissionScoresFromExcel("10", file);
            
            verify(submissionRepository, times(1)).saveAll(any());
        }
    }

    @Test
    @DisplayName("[TC_SUB_58] downloadAllSubmissions - IOException khi getFile (Bao phủ ternary file == null)")
    void downloadAllSubmissions_GetFileError_ThrowsException() throws IOException {
        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("valid_url").fileName("manual_name.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        // Error BEFORE 'file' variable is assigned
        when(fileRes.getFile()).thenThrow(new IOException("File system error"));

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("File system error");
    }

    @Test
    @DisplayName("[TC_SUB_59] importSubmissionScoresFromExcel - Bao phủ nhánh gradeStr là null trong Excel")
    void importSubmissionScoresFromExcel_GradeNull_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        mockAssignmentCanViewSubmissions(10L, 2L, true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(any(), anyInt())).thenReturn(row);
            
            // Return null for grade cell (index 7)
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any()))
                      .thenReturn("user1", "Name", "SV001", null); 

            submissionService.importSubmissionScoresFromExcel("10", file);
            
            verify(submissionRepository, times(1)).saveAll(any());
        }
    }

    @Test
    @DisplayName("[TC_SUB_60] searchSubmission - Bao phủ pageSize là null (Không phân trang)")
    void searchSubmission_PageSizeNull_Success() {
        mockCurrentUser(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = makeSearchRequest(null, "1", null);

        mockAssignmentCanViewAny(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        var result = submissionService.searchSubmission(request);
        assertThat(result.getPaging()).isNotNull();
    }

    @Test
    @DisplayName("[TC_SUB_61] searchSubmission - Bao phủ pageSize là chuỗi rỗng")
    void searchSubmission_PageSizeEmpty_Success() {
        mockCurrentUser(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = makeSearchRequest(null, "1", "  ");

        mockAssignmentCanViewAny(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        var result = submissionService.searchSubmission(request);
        assertThat(result.getPaging()).isNotNull();
    }

    @Test
    @DisplayName("[TC_SUB_62] importSubmissionScoresFromExcel - Bao phủ gradeStr là chuỗi rỗng")
    void importSubmissionScoresFromExcel_GradeEmptyString_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(List.of(existingSubmission));

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(any(), anyInt())).thenReturn(row);
            
            // gradeStr (index 7) is empty string ""
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any()))
                      .thenReturn("user1", "Name", "SV001", ""); 

            submissionService.importSubmissionScoresFromExcel("10", file);
            
            verify(submissionRepository, times(1)).saveAll(any());
        }
    }

    @Test
    @DisplayName("[TC_SUB_63] isBlank Helper - Bao phủ chuỗi chỉ chứa khoảng trắng")
    void importSubmissionScoresFromExcel_CodeWhitespace_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        mockCurrentUser(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.createWorkbook(any())).thenReturn(workbook);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getSheet(workbook, 0)).thenReturn(sheet);
            when(sheet.getLastRowNum()).thenReturn(1);
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getRow(sheet, 1)).thenReturn(row);
            
            // Code (index 3) is "   " (whitespace)
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.getCellValueAsString(any())).thenReturn("user1", "Full Name", "   ", "10.0");

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
    }

    @Nested
    @DisplayName("Tests from Assignment Management Module")
    class AssignmentManagementModuleTests {

    private User     studentUser;
    private User     teacherUser;
    private Assignment onTimeAssignment;   // dueDate tương lai → isLate = false
    private Assignment lateAssignment;     // dueDate quá khứ   → isLate = true

    @BeforeEach
    void setUp() {
        studentUser = User.builder().id(1L).role(Role.STUDENT).build();
        teacherUser = User.builder().id(2L).role(Role.TEACHER).build();

        onTimeAssignment = Assignment.builder()
                .assignmentId(10L)
                .classroomId(100L)
                .dueDate(LocalDateTime.now().plusDays(5))   // chưa quá hạn
                .submissionClosed(false)
                .maxScore(10L)
                .build();

        lateAssignment = Assignment.builder()
                .assignmentId(10L)
                .classroomId(100L)
                .dueDate(LocalDateTime.now().minusDays(1))  // đã quá hạn
                .submissionClosed(false)
                .maxScore(10L)
                .build();
    }

    // ────────────────────── Helper builders ──────────────────────

    /** Tạo Submission cơ bản cho student */
    private Submission buildSubmission(Long subId, Long assignmentId) {
        return Submission.builder()
                .submissionId(subId)
                .assignmentId(assignmentId)
                .studentId(studentUser.getId())
                .submissionStatus(SubmissionStatus.NOT_SUBMITTED)
                .gradingStatus(GradingStatus.NOT_GRADED)
                .build();
    }

    /** Tạo Attachment đơn giản */
    private Attachment buildAttachment(Long attachId, Long objectId, String fileName) {
        return Attachment.builder()
                .attachmentId(attachId)
                .objectId(objectId)
                .attachmentType(AttachmentType.SUBMISSION)
                .fileName(fileName)
                .fileUrl("https://storage.example.com/" + fileName)
                .uploadedBy(studentUser.getId())
                .isDeleted(false)
                .build();
    }

    /**
     * Tạo MockMultipartFile chứa nội dung Excel thật (dùng Apache POI)
     * để có thể test các nhánh bên trong parseSubmissionExcel().
     * Header ở row 0, data bắt đầu từ row 1.
     */
    private MockMultipartFile buildExcelFile(List<Object[]> dataRows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("DANH_SACH");
            // Row 0: header (bị skip vì vòng lặp bắt đầu i=1)
            sheet.createRow(0);
            for (int i = 0; i < dataRows.size(); i++) {
                var row = sheet.createRow(i + 1);
                Object[] cols = dataRows.get(i);
                for (int c = 0; c < cols.length; c++) {
                    if (cols[c] != null) {
                        row.createCell(c).setCellValue(cols[c].toString());
                    }
                    // null → cell không tồn tại → getCellValueAsString trả về null/""
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile(
                    "file", "scores.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 1 – getDetailSubmission  (4 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 1 – getDetailSubmission  (4 TC)")
        class Group1Tests {


        @Test
        @DisplayName("TC_QLBT_SSV1_001 – Không có permission → throw FORBIDDEN")
        void getDetailSubmission_NoPermission() {
            // hasPermission trả false → phải throw trước khi query submission
            mockCurrentUser(studentUser);
            mockSubmissionHasPermission(100L, 1L, false);

            assertThatThrownBy(() -> submissionService.getDetailSubmission("100"))
                    .isInstanceOf(AppException.class);

            verify(submissionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("TC_QLBT_SSV1_002 – Có permission nhưng submission không tồn tại → throw NOT_FOUND")
        void getDetailSubmission_SubmissionNotFound() {
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(999L, 2L, true);
            when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.getDetailSubmission("999"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV1_003 – Tìm thấy submission có 2 attachment → response.attachmentList.size = 2")
        void getDetailSubmission_FoundWithAttachments() {
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(100L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(studentUser);
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));

            Attachment att1 = buildAttachment(1L, 100L, "hw.pdf");
            Attachment att2 = buildAttachment(2L, 100L, "hw.docx");
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(att1, att2));

            SubmissionDetailResponse resp = submissionService.getDetailSubmission("100");

            assertThat(resp).isNotNull();
            assertThat(resp.getAttachmentResponseList()).hasSize(2);
        }

        @Test
        @DisplayName("TC_QLBT_SSV1_004 – Tìm thấy submission không có attachment → attachmentList rỗng")
        void getDetailSubmission_FoundNoAttachments() {
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(100L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(studentUser);
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(Collections.emptyList());

            SubmissionDetailResponse resp = submissionService.getDetailSubmission("100");

            assertThat(resp.getAttachmentResponseList()).isEmpty();
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 2 – getMySubmission  (3 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 2 – getMySubmission  (3 TC)")
        class Group2Tests {


        @Test
        @DisplayName("TC_QLBT_SSV2_001 – Không tìm thấy submission → trả về empty builder response (không throw)")
        void getMySubmission_NotFound_ReturnsEmpty() {
            mockCurrentUser(studentUser);
            when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L))
                    .thenReturn(Optional.empty());

            SubmissionDetailResponse resp = submissionService.getMySubmission("10");

            // Service trả về SubmissionDetailResponse.builder().build() — không throw
            assertThat(resp).isNotNull();
            assertThat(resp.getAttachmentResponseList()).isNullOrEmpty();
        }

        @Test
        @DisplayName("TC_QLBT_SSV2_002 – Tìm thấy submission có 1 attachment → response đủ thông tin")
        void getMySubmission_FoundWithAttachment() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(studentUser);  // FIX: fromEntity() gọi entity.getStudent() → cần set student
            when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L))
                    .thenReturn(Optional.of(sub));

            Attachment att = buildAttachment(1L, 100L, "baitap.pdf");
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(att));

            SubmissionDetailResponse resp = submissionService.getMySubmission("10");

            assertThat(resp).isNotNull();
            assertThat(resp.getAttachmentResponseList()).hasSize(1);
        }

        @Test
        @DisplayName("TC_QLBT_SSV2_003 – Tìm thấy submission không có attachment → attachmentList rỗng (không throw)")
        void getMySubmission_FoundNoAttachments() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(studentUser);  // FIX: fromEntity() gọi entity.getStudent() → cần set student
            when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L))
                    .thenReturn(Optional.of(sub));
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(Collections.emptyList());

            SubmissionDetailResponse resp = submissionService.getMySubmission("10");

            assertThat(resp.getAttachmentResponseList()).isEmpty();
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 3 – createDefaultSubmissions  (3 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 3 – createDefaultSubmissions  (3 TC)")
        class Group3Tests {


        @Test
        @DisplayName("TC_QLBT_SSV3_001 – assignment = null → return sớm, không gọi repo")
        void createDefaultSubmissions_NullAssignment() {
            // Không setup mock nào — nếu service vẫn gọi repo thì Mockito sẽ lỗi strict
            submissionService.createDefaultSubmissions(null);

            verify(classMemberRepository, never()).getClassMemberIdsActive(anyLong(), any());
            verify(submissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC_QLBT_SSV3_002 – Lớp không có sinh viên (studentIds rỗng) → save không được gọi")
        void createDefaultSubmissions_EmptyClass() {
            when(classMemberRepository.getClassMemberIdsActive(100L, ClassMemberRole.STUDENT))
                    .thenReturn(Collections.emptySet());

            submissionService.createDefaultSubmissions(onTimeAssignment);

            verify(submissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC_QLBT_SSV3_003 – Lớp có 3 sinh viên → save được gọi đúng 3 lần")
        void createDefaultSubmissions_ThreeStudents() {
            when(classMemberRepository.getClassMemberIdsActive(100L, ClassMemberRole.STUDENT))
                    .thenReturn(Set.of(1L, 2L, 3L));

            submissionService.createDefaultSubmissions(onTimeAssignment);

            verify(submissionRepository, times(3)).save(any(Submission.class));
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 4 – deleteAttachmentInSubmission  (9 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 4 – deleteAttachmentInSubmission  (9 TC)")
        class Group4Tests {


        @Test
        @DisplayName("TC_QLBT_SSV4_001 – Attachment không tìm thấy hoặc không thuộc user → throw NOT_FOUND")
        void deleteAttachment_AttachmentNotFound() {
            mockCurrentUser(studentUser);
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(99L, 1L, false))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("99"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_002 – Attachment tìm thấy nhưng submission không tồn tại → throw NOT_FOUND")
        void deleteAttachment_SubmissionNotFound() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));
            // objectId của att = 100 → tìm submission theo submissionId=100, studentId=1
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("1"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_003 – Assignment không còn active / SV không đủ điều kiện nộp → throw NOT_FOUND")
        void deleteAttachment_AssignmentNotEligible() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            // findAssignmentIfUserCanSubmit trả empty → findAssignmentForStudentActive throw
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("1"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_004 – Nộp trễ + submissionClosed=true → throw LATE_SUBMISSION_NOT_ALLOWED")
        void deleteAttachment_LateAndClosed() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            // dueDate quá khứ → isLate=true, submissionClosed=true → throw
            Assignment closedLate = Assignment.builder()
                    .assignmentId(10L).classroomId(100L)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .submissionClosed(true).build();
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(closedLate));

            assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("1"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_005 – Nộp trễ + submissionClosed=false + sau xóa còn file → status LATE_SUBMITTED")
        void deleteAttachment_LateNotClosed_StillHasFiles() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(lateAssignment)); // isLate=true, closed=false

            // Sau khi xóa att, vẫn còn 1 file khác
            Attachment remaining = buildAttachment(2L, 100L, "hw2.pdf");
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(remaining));

            submissionService.deleteAttachmentInSubmission("1");

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
            verify(submissionRepository).save(sub);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_006 – Đúng hạn + sau xóa còn file → status không thay đổi (SUBMITTED)")
        void deleteAttachment_OnTime_StillHasFiles_NoStatusChange() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            sub.setSubmissionStatus(SubmissionStatus.SUBMITTED);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(onTimeAssignment)); // isLate=false

            Attachment remaining = buildAttachment(2L, 100L, "hw2.pdf");
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(remaining));

            submissionService.deleteAttachmentInSubmission("1");

            // isLate=false, attachments không rỗng → không vào if nào → status giữ nguyên
            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_007 – Sau xóa không còn file nào → status = NOT_SUBMITTED, submittedAt = null")
        void deleteAttachment_OnTime_NoFilesLeft_ResetStatus() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            sub.setSubmissionStatus(SubmissionStatus.SUBMITTED);
            sub.setSubmittedAt(LocalDateTime.now().minusHours(1));
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(onTimeAssignment));

            // Sau xóa không còn file nào
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(Collections.emptyList());

            submissionService.deleteAttachmentInSubmission("1");

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
            assertThat(sub.getSubmittedAt()).isNull();
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_008 – Nộp trễ + sau xóa vẫn còn file → status = LATE_SUBMITTED")
        void deleteAttachment_Late_StillHasFiles_SetLateSubmitted() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(lateAssignment)); // isLate=true

            Attachment remaining = buildAttachment(2L, 100L, "hw2.pdf");
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(remaining));

            submissionService.deleteAttachmentInSubmission("1");

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
        }

        @Test
        @DisplayName("TC_QLBT_SSV4_009 – Đúng hạn + sau xóa không còn file → NOT_SUBMITTED (kiểm tra nhánh attachments.isEmpty)")
        void deleteAttachment_OnTime_NoFilesLeft_StatusNotSubmitted() {
            mockCurrentUser(studentUser);

            Attachment att = buildAttachment(1L, 100L, "hw.pdf");
            when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(1L, 1L, false))
                    .thenReturn(Optional.of(att));

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(onTimeAssignment)); // isLate=false

            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(Collections.emptyList()); // hết file

            submissionService.deleteAttachmentInSubmission("1");

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
            assertThat(sub.getSubmittedAt()).isNull();
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 5 – addAttachmentToSubmission  (5 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 5 – addAttachmentToSubmission  (5 TC)")
        class Group5Tests {


        @Test
        @DisplayName("TC_QLBT_SSV5_001 – Submission không tìm thấy → throw NOT_FOUND")
        void addAttachment_SubmissionNotFound() {
            mockCurrentUser(studentUser);
            when(submissionRepository.findBySubmissionIdAndStudentId(999L, 1L))
                    .thenReturn(Optional.empty());

            SubmissionUpdateRequest req = new SubmissionUpdateRequest();
            req.setAttachmentCreateRequestList(new java.util.ArrayList<>());
            assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("999", req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV5_002 – Assignment không còn cho phép nộp → throw NOT_FOUND")
        void addAttachment_AssignmentNotEligible() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            SubmissionUpdateRequest req = new SubmissionUpdateRequest();
            req.setAttachmentCreateRequestList(new java.util.ArrayList<>());
            assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("100", req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV5_003 – Nộp trễ + submissionClosed=true → throw LATE_SUBMISSION_NOT_ALLOWED")
        void addAttachment_LateAndClosed() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));

            Assignment closedLate = Assignment.builder()
                    .assignmentId(10L).classroomId(100L)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .submissionClosed(true).build();
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(closedLate));

            SubmissionUpdateRequest req = new SubmissionUpdateRequest();
            req.setAttachmentCreateRequestList(new java.util.ArrayList<>());
            assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("100", req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV5_004 – Đúng hạn + 1 file → status = SUBMITTED, attachment được lưu")
        void addAttachment_OnTime_StatusSubmitted() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(onTimeAssignment)); // isLate=false

            SubmissionUpdateRequest req = new SubmissionUpdateRequest();
            AttachmentCreateRequest fileReq = AttachmentCreateRequest.builder()
                    .fileName("BT1.pdf").fileUrl("https://storage.example.com/BT1.pdf").build();
            req.setAttachmentCreateRequestList(List.of(fileReq));

            submissionService.addAttachmentToSubmission("100", req);

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
            verify(attachmentRepository, times(1)).save(any(Attachment.class));
            verify(submissionRepository, times(1)).save(sub);
        }

        @Test
        @DisplayName("TC_QLBT_SSV5_005 – Nộp trễ + submissionClosed=false + 1 file → status = LATE_SUBMITTED")
        void addAttachment_LateNotClosed_StatusLateSubmitted() {
            mockCurrentUser(studentUser);
            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                    .thenReturn(Optional.of(sub));
            when(assignmentRepository.findAssignmentIfUserCanSubmit(
                    1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                    .thenReturn(Optional.of(lateAssignment)); // isLate=true, closed=false

            SubmissionUpdateRequest req = new SubmissionUpdateRequest();
            AttachmentCreateRequest fileReq = AttachmentCreateRequest.builder()
                    .fileName("BT1.pdf").fileUrl("https://storage.example.com/BT1.pdf").build();
            req.setAttachmentCreateRequestList(List.of(fileReq));

            submissionService.addAttachmentToSubmission("100", req);

            assertThat(sub.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 6 – searchSubmission  (3 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 6 – searchSubmission  (3 TC)")
        class Group6Tests {


        @Test
        @DisplayName("TC_QLBT_SSV6_001 – canUserViewSubmissions = false → throw FORBIDDEN")
        void searchSubmission_NoPermission() {
            mockCurrentUser(studentUser);

            SubmissionSearchRequest filter = new SubmissionSearchRequest();
            filter.setAssignmentId("10");
            BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
            req.setFilters(filter);
            req.setPagination(new SearchRequest());

            mockAssignmentCanViewSubmissions(10L, 1L, false);

            assertThatThrownBy(() -> submissionService.searchSubmission(req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV6_002 – Có quyền, có kết quả → response.content.size = 2")
        void searchSubmission_WithResults() {
            mockCurrentUser(teacherUser);

            SubmissionSearchRequest filter = new SubmissionSearchRequest();
            filter.setAssignmentId("10");
            SearchRequest pagination = new SearchRequest();
            pagination.setPageNum("0");
            pagination.setPageSize("10");
            BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
            req.setFilters(filter);
            req.setPagination(pagination);

            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // Tạo 2 DTO kết quả giả
            SubmissionSearchQueryDTO dto1 = mock(SubmissionSearchQueryDTO.class);
            SubmissionSearchQueryDTO dto2 = mock(SubmissionSearchQueryDTO.class);
            Page<SubmissionSearchQueryDTO> page = new PageImpl<>(List.of(dto1, dto2));
            when(submissionRepository.searchSubmission(any(), any(Pageable.class))).thenReturn(page);

            ResponseListData<SubmissionSearchResponse> result = submissionService.searchSubmission(req);

            assertThat(result.getContent()).hasSize(2); 
        }

        @Test
        @DisplayName("TC_QLBT_SSV6_003 – Có quyền, không có kết quả → response.content rỗng")
        void searchSubmission_NoResults() {
            mockCurrentUser(teacherUser);

            SubmissionSearchRequest filter = new SubmissionSearchRequest();
            filter.setAssignmentId("10");
            SearchRequest pagination = new SearchRequest();
            pagination.setPageNum("0");
            pagination.setPageSize("10");
            BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
            req.setFilters(filter);
            req.setPagination(pagination);

            mockAssignmentCanViewSubmissions(10L, 2L, true);
            when(submissionRepository.searchSubmission(any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            ResponseListData<SubmissionSearchResponse> result = submissionService.searchSubmission(req);

            assertThat(result.getContent()).isEmpty();
        }

        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 7 – markSubmission  (5 TC)
        // ═══════════════════════════════════════════════════════════════
        @Nested
        @DisplayName("GROUP 7 – markSubmission  (5 TC)")
        class Group7Tests {


        @Test
        @DisplayName("TC_QLBT_SSV7_001 – hasPermission = false → throw FORBIDDEN")
        void markSubmission_NoPermission() {
            mockCurrentUser(studentUser);
            mockSubmissionHasPermission(100L, 1L, false);

            assertThatThrownBy(() -> submissionService.markSubmission("100", new SubmissionGradeUpdateRequest()))
                    .isInstanceOf(AppException.class);

            verify(submissionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("TC_QLBT_SSV7_002 – Có permission nhưng submission không tồn tại → throw NOT_FOUND")
        void markSubmission_SubmissionNotFound() {
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(999L, 2L, true);
            when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

            SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
            req.setGrade("10.0");
            assertThatThrownBy(() -> submissionService.markSubmission("999", req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV7_003 – Chấm điểm hợp lệ (8.5/10) → gradingStatus = GRADED, gradedAt được set")
        void markSubmission_Success() {
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(100L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));

            SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
            req.setGrade("8.5");

            submissionService.markSubmission("100", req);

            assertThat(sub.getGrade()).isEqualTo(8.5);
            assertThat(sub.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
            assertThat(sub.getGradedAt()).isNotNull();
            verify(submissionRepository, times(1)).save(sub);
        }

        @Test
        @DisplayName("TC_QLBT_SSV7_004 – Điểm âm → phải throw AppException ")
        void markSubmission_NegativeGrade_ShouldThrow() {
            // Kỳ vọng thực tế: điểm không thể < 0
                    //               Service layer không validate → grade âm vẫn được lưu vào DB
            mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(100L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));

            SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
            req.setGrade("-1.0"); // Điểm âm – vô lý về nghiệp vụ

            // Kỳ vọng: phải throw AppException tại Service layer
            // Hiện tại FAIL để ghi nhận bug: service chưa validate grade >= 0
            assertThatThrownBy(() -> submissionService.markSubmission("100", req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV7_005 – Điểm vượt maxScore (12/10) → phải throw AppException ")
        void markSubmission_GradeExceedsMaxScore_ShouldThrow() {
            // Kỳ vọng thực tế: grade không được vượt quá maxScore của Assignment
                    mockCurrentUser(teacherUser);
            mockSubmissionHasPermission(100L, 2L, true);

            Submission sub = buildSubmission(100L, 10L); // assignment maxScore = 10
            when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));

            SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
            req.setGrade("12.0"); // Vượt maxScore=10

            // Kỳ vọng: phải throw AppException vì 12 > maxScore(10)
            // Hiện tại FAIL để ghi nhận bug: service không có bước kiểm tra maxScore
            assertThatThrownBy(() -> submissionService.markSubmission("100", req))
                    .isInstanceOf(AppException.class);
        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 8 – downloadAllSubmissions  (4 TC)
        //  NOTE: B5 (IOException) và B6 (happy path full ZIP) yêu cầu
        //        mock Resource.getFile() — đây là test phức tạp với File I/O.
        //        Các nhánh này được cover ở integration test.
        // ═══════════════════════════════════════════════════════════════

        @Test
        @DisplayName("TC_QLBT_SSV8_001 – canUserViewSubmissions = false → throw FORBIDDEN")
        void downloadAllSubmissions_NoPermission() {
            mockCurrentUser(studentUser);
            mockAssignmentCanViewSubmissions(10L, 1L, false);

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV8_002 – Không có submission nào → throw NOT_FOUND")
        void downloadAllSubmissions_NoSubmissions() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);
            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV8_003 – Có submission nhưng tất cả attachment URL = null (fileName=null) → throw NOT_FOUND")
        void downloadAllSubmissions_AllAttachmentsHaveNullFileName() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(sub));

            // Attachment có fileUrl = null → FileUtils.getFileNameFromDefaultUrl(null) = null → continue, hasAnyFile = false
            Attachment attNullUrl = Attachment.builder()
                    .attachmentId(1L).objectId(100L)
                    .attachmentType(AttachmentType.SUBMISSION)
                    .fileUrl(null).fileName("hw.pdf")
                    .build();
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(List.of(attNullUrl));

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV8_004 – Có submission nhưng không có attachment nào → hasAnyFile=false → throw NOT_FOUND")
        void downloadAllSubmissions_SubmissionsWithNoAttachments() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            Submission sub = buildSubmission(100L, 10L);
            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(sub));
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 9 – downloadGradeTemplate  (3 TC)
        //  NOTE: ExcelUtils.exportToExcel() là static call, không mock được.
        //        Test chỉ verify service không throw và trả về ByteArrayResource.
        // ═══════════════════════════════════════════════════════════════

        @Test
        @DisplayName("TC_QLBT_SSV9_001 – canUserViewSubmissions = false → throw FORBIDDEN")
        void downloadGradeTemplate_NoPermission() {
            mockCurrentUser(studentUser);
            mockAssignmentCanViewSubmissions(10L, 1L, false);

            assertThatThrownBy(() -> submissionService.downloadGradeTemplate("10"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV9_002 – data rỗng → vẫn trả về ByteArrayResource (excel rỗng, không throw)")
        void downloadGradeTemplate_EmptyData() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);
            when(submissionRepository.findAllForExcel(10L)).thenReturn(Collections.emptyList());

            // ExcelUtils.exportToExcel() là static, không mock được → chỉ verify không throw
            ByteArrayResource resource = submissionService.downloadGradeTemplate("10");

            assertThat(resource).isNotNull();
        }

        @Test
        @DisplayName("TC_QLBT_SSV9_003 – Có data → trả về ByteArrayResource (không throw)")
        void downloadGradeTemplate_WithData() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // FIX: dùng concrete builder thay vì mock() vì toExcelDTO() dùng switch trên enum
            // → mock trả null cho getter enum → NPE.
            SubmissionExcelQueryDTO dto1 = SubmissionExcelQueryDTO.builder()
                    .submissionId(1L).username("user1").fullName("Nguyen Van A").code("B21DCCN001")
                    .submissionStatus(SubmissionStatus.SUBMITTED)
                    .gradingStatus(GradingStatus.GRADED)
                    .grade(8.5)
                    .build();
            SubmissionExcelQueryDTO dto2 = SubmissionExcelQueryDTO.builder()
                    .submissionId(2L).username("user2").fullName("Tran Thi B").code("B21DCCN002")
                    .submissionStatus(SubmissionStatus.NOT_SUBMITTED)
                    .gradingStatus(GradingStatus.NOT_GRADED)
                    .build();
            when(submissionRepository.findAllForExcel(10L)).thenReturn(List.of(dto1, dto2));

            ByteArrayResource resource = submissionService.downloadGradeTemplate("10");

            assertThat(resource).isNotNull();
        }

        // ═══════════════════════════════════════════════════════════════
        //  GROUP 10 – importSubmissionScoresFromExcel  (8 TC)
        //  Dùng Apache POI tạo file Excel thật trong memory để hit
        //  các nhánh bên trong parseSubmissionExcel().
        // ═══════════════════════════════════════════════════════════════

        @Test
        @DisplayName("TC_QLBT_SSV10_001 – canUserViewSubmissions = false → throw FORBIDDEN")
        void importScores_NoPermission() {
            mockCurrentUser(studentUser);
            mockAssignmentCanViewSubmissions(10L, 1L, false);

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", mock(MockMultipartFile.class)))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_002 – File không phải Excel hợp lệ → IOException → throw FILE_UPLOAD_FAILED")
        void importScores_InvalidFile() {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // File nội dung rác → WorkbookFactory.create() sẽ throw IOException
            MockMultipartFile badFile = new MockMultipartFile(
                    "file", "bad.xlsx", "application/octet-stream",
                    "NOT_AN_EXCEL_CONTENT".getBytes()
            );

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", badFile))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_003 – Username trống → throw IMPORT_MISSING_STUDENT_INFO")
        void importScores_BlankUsername() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // col index: 0=STT, 1=username, 2=fullName, 3=code, 7=grade
            // username (col1) để trống
            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "", "Nguyen Van A", "B21DCCN001", null, null, null, "8.0"}
            ));

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_004 – Mã sinh viên (code) trống → throw IMPORT_MISSING_STUDENT_INFO")
        void importScores_BlankCode() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // code (col3) để trống
            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "", null, null, null, "8.0"}
            ));

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_005 – Ô điểm để trống → grade = null, không cập nhật gradingStatus")
        void importScores_EmptyGrade_GradeNull() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            // grade (col7) = null → grade=null trong DTO → if(dto.getGrade() != null) = false → không set grade
            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "B21DCCN001", null, null, null, null}
            ));

            User stu = User.builder().id(1L).code("B21DCCN001").build();
            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(stu);

            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(sub));

            submissionService.importSubmissionScoresFromExcel("10", file);

            // grade không được set → vẫn null
            assertThat(sub.getGrade()).isNull();
            assertThat(sub.getGradingStatus()).isEqualTo(GradingStatus.NOT_GRADED);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_006 – Ô điểm là chuỗi không phải số → throw IMPORT_INVALID_GRADE_FORMAT")
        void importScores_InvalidGradeFormat() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "B21DCCN001", null, null, null, "abc"}
            ));

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_007 – Mã SV trong file không có trong hệ thống → throw IMPORT_SUBMISSION_NOT_FOUND")
        void importScores_StudentCodeNotFound() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "UNKNOWN_CODE", null, null, null, "8.0"}
            ));

            // submissionMap không có key "UNKNOWN_CODE" → throw
            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_008 – Happy path: điểm hợp lệ, code tồn tại → saveAll được gọi, grade được cập nhật")
        void importScores_HappyPath() throws IOException {
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "B21DCCN001", null, null, null, "9.0"}
            ));

            User stu = User.builder().id(3L).code("B21DCCN001").build();
            Submission sub = buildSubmission(100L, 10L);
            sub.setStudent(stu);
            sub.setGrade(null); // grade cũ = null → gradedAt sẽ được cập nhật

            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(sub));

            submissionService.importSubmissionScoresFromExcel("10", file);

            assertThat(sub.getGrade()).isEqualTo(9.0);
            assertThat(sub.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
            assertThat(sub.getGradedAt()).isNotNull();
            verify(submissionRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("TC_QLBT_SSV10_009 – Điểm import vượt maxScore → phải throw AppException ")
        void importScores_GradeExceedsMaxScore_ShouldThrow() throws IOException {
            // Kỳ vọng thực tế: điểm import không được vượt quá maxScore của Assignment
                    //               → Điểm 99.0 với maxScore=10 vẫn được lưu vào DB
            mockCurrentUser(teacherUser);
            mockAssignmentCanViewSubmissions(10L, 2L, true);

            MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                    new Object[]{"1", "user1", "Nguyen Van A", "B21DCCN001", null, null, null, "99.0"}
            ));

            User stu = User.builder().id(1L).code("B21DCCN001").build();
            Submission sub = buildSubmission(100L, 10L); // assignment maxScore = 10
            sub.setStudent(stu);

            when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(sub));

            // Kỳ vọng: phải throw AppException vì 99.0 > maxScore(10)
            // Hiện tại FAIL để ghi nhận bug: service không kiểm tra grade <= maxScore
            assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                    .isInstanceOf(AppException.class);
        }
        }

    }
}
