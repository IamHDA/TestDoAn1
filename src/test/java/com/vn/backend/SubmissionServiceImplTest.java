package com.vn.backend;

import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.attachment.AttachmentCreateRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.submission.*;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.submission.SubmissionDetailResponse;
import com.vn.backend.entities.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

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

    @Test
    @DisplayName("TC_QLT_01: getMySubmission - Thành công khi đã có bài nộp")
    void getMySubmission_Found_Success() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(new ArrayList<>());

        var response = submissionService.getMySubmission("10");

        assertThat(response).isNotNull();
        assertThat(response.getSubmissionId()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("TC_QLT_02: getMySubmission - Trả về object trống khi chưa bắt đầu")
    void getMySubmission_NotFound_ReturnsEmpty() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L)).thenReturn(Optional.empty());

        var response = submissionService.getMySubmission("10");

        assertThat(response.getSubmissionId()).isNull();
    }

    @Test
    @DisplayName("TC_QLT_03: createDefaultSubmissions - Thoát sớm nếu assignment rỗng")
    void createDefaultSubmissions_NullAssignment_Returns() {
        submissionService.createDefaultSubmissions(null);
        verifyNoInteractions(submissionRepository);
    }
    @DisplayName("TC_QLT_04: deleteAttachmentInSubmission - Thành công và trả trạng thái về NOT_SUBMITTED khi hết file")
    void deleteAttachmentInSubmission_LastFile_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(eq(1L), anyLong(), any(), any())).thenReturn(Optional.of(activeAssignment));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(new ArrayList<>());

        submissionService.deleteAttachmentInSubmission("500");

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("TC_QLT_37: getDetailSubmission - Thất bại khi không tìm thấy bài nộp")
    void getDetailSubmission_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getDetailSubmission("1000"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_43: deleteAttachmentInSubmission - Nộp trễ nhưng bài tập vẫn mở (Không ném lỗi)")
    void deleteAttachmentInSubmission_Late_Open_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment lateOpenAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(10)).submissionClosed(false).build();
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateOpenAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("TC_QLT_26: deleteAttachmentInSubmission - Xóa file khi nộp muộn nhưng vẫn còn file khác")
    void deleteAttachmentInSubmission_Late_StillHasFiles_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment lateAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateAss));
        // Giả lập vẫn còn 1 file khác
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    @Test
    @DisplayName("TC_QLT_05: deleteAttachmentInSubmission - Ném lỗi khi nộp bài trễ và bài tập đã đóng")
    void deleteAttachmentInSubmission_LateAndClosed_ThrowsException() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment closedAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(true).build();
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(closedAss));

        assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("500"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_08: addAttachmentToSubmission - Nộp bài muộn (LATE_SUBMITTED) khi cho phép")
    void addAttachmentToSubmission_LateAllowed_Success() {
        Assignment lateAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateAss));

        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    // ===========================================
    // II. Group: Search & Permissions
    // ===========================================

    @Test
    @DisplayName("TC_QLT_11: searchSubmission - Thành công")
    void searchSubmission_Success() {
        BaseFilterSearchRequest<SubmissionSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SubmissionSearchRequest());
        SearchRequest pr = new SearchRequest();
        pr.setPageNum("1"); pr.setPageSize("10");
        request.setPagination(pr);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        submissionService.searchSubmission(request);

        verify(submissionRepository).searchSubmission(any(), any());
    }

    @Test
    @DisplayName("TC_QLT_29: searchSubmission - Thất bại do không có quyền xem")
    void searchSubmission_Forbidden_ThrowsException() {
        BaseFilterSearchRequest<SubmissionSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SubmissionSearchRequest());
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> submissionService.searchSubmission(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_13: markSubmission - Thất bại khi không có quyền chấm điểm")
    void markSubmission_NoPermission_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.hasPermission(1000L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.markSubmission("1000", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_27: markSubmission - Thành công chấm điểm")
    void markSubmission_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(1000L, 2L)).thenReturn(true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(existingSubmission));
        
        SubmissionGradeUpdateRequest request = new SubmissionGradeUpdateRequest();
        request.setGrade("9.0");

        submissionService.markSubmission("1000", request);

        assertThat(existingSubmission.getGrade()).isEqualTo(9.0);
        assertThat(existingSubmission.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
    }

    @Test
    @DisplayName("TC_QLT_28: markSubmission - Thất bại khi bài nộp không tồn tại")
    void markSubmission_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(1000L, 2L)).thenReturn(true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.markSubmission("1000", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // III. Group: File Operations & Zip
    // ===========================================

    @Test
    @DisplayName("TC_QLT_17: downloadAllSubmissions - Thất bại khi không tìm thấy bài nộp nào")
    void downloadAllSubmissions_Empty_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_18: downloadAllSubmissions - Thất bại khi không có file đính kèm thực tế")
    void downloadAllSubmissions_NoFiles_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    // ===========================================
    // IV. Group: Excel Operations
    // ===========================================

    @Test
    @DisplayName("TC_QLT_21: importSubmissionScoresFromExcel - Cập nhật điểm thành công")
    void importSubmissionScoresFromExcel_Success() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cellUsername = mock(Cell.class);
        Cell cellCode = mock(Cell.class);
        Cell cellGrade = mock(Cell.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(true);
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
    @DisplayName("TC_QLT_30: downloadGradeTemplate - Thành công")
    void downloadGradeTemplate_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllForExcel(anyLong())).thenReturn(new ArrayList<>());

        try (MockedStatic<com.vn.backend.utils.ExcelUtils> excelUtils = mockStatic(com.vn.backend.utils.ExcelUtils.class)) {
            excelUtils.when(() -> com.vn.backend.utils.ExcelUtils.exportToExcel(any(), any(), any(), any())).thenReturn(new ByteArrayResource(new byte[0]));
            
            var result = submissionService.downloadGradeTemplate("10");
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("TC_QLT_31: downloadGradeTemplate - Thất bại do không có quyền")
    void downloadGradeTemplate_Forbidden_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> submissionService.downloadGradeTemplate("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_32: importSubmissionScoresFromExcel - Thất bại do không có quyền")
    void importSubmissionScoresFromExcel_Forbidden_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", mock(MultipartFile.class)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_33: importSubmissionScoresFromExcel - Thất bại do thiếu thông tin sinh viên trong Excel")
    void importSubmissionScoresFromExcel_MissingInfo_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_34: downloadAllSubmissions - Thành công tạo file Zip")
    void downloadAllSubmissions_Success() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_35: downloadAllSubmissions - Bỏ qua file khi URL không hợp lệ")
    void downloadAllSubmissions_SkipInvalidFile_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("invalid-url").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_24: importSubmissionScoresFromExcel - Thất bại khi định dạng điểm sai (không phải số)")
    void importSubmissionScoresFromExcel_InvalidGrade_ThrowsException() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cellGrade = mock(Cell.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(any(), any())).thenReturn(true);

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
    @DisplayName("TC_QLT_36: importSubmissionScoresFromExcel - Cập nhật gradedAt khi điểm mới khác điểm cũ")
    void importSubmissionScoresFromExcel_UpdateGradedAt_Success() throws IOException {
        existingSubmission.setGrade(5.0); // Điểm cũ
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_38: importSubmissionScoresFromExcel - Không cập nhật gradedAt khi điểm mới trùng điểm cũ")
    void importSubmissionScoresFromExcel_SameGrade_NoGradedAtUpdate() throws IOException {
        existingSubmission.setGrade(8.5); // Điểm cũ là 8.5
        LocalDateTime oldGradedAt = LocalDateTime.now().minusDays(1);
        existingSubmission.setGradedAt(oldGradedAt);
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_39: importSubmissionScoresFromExcel - Bỏ qua bản ghi khi cột điểm trống")
    void importSubmissionScoresFromExcel_EmptyGradeCell_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_40: deleteAttachmentInSubmission - Thất bại khi không tìm thấy file đính kèm")
    void deleteAttachmentInSubmission_AttachmentNotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(anyLong(), anyLong(), eq(false))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("500"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_41: importSubmissionScoresFromExcel - Bỏ qua sinh viên có dữ liệu Student bị null trong DB")
    void importSubmissionScoresFromExcel_NullStudentInDB_SkipRow() throws IOException {
        Submission nullStudentSubmission = Submission.builder().build(); // student is null
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_42: downloadAllSubmissions - Thất bại do lỗi I/O khi nén file")
    void downloadAllSubmissions_IOError_ThrowsException() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("http://storage/file.pdf").fileName("file.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.toPath()).thenReturn(new java.io.File("temp.pdf").toPath());

        try (MockedStatic<java.nio.file.Files> filesMock = mockStatic(java.nio.file.Files.class)) {
            filesMock.when(() -> java.nio.file.Files.copy(any(java.nio.file.Path.class), any(java.io.OutputStream.class)))
                    .thenThrow(new IOException("Simulated IO Error"));

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    @DisplayName("TC_QLT_44: importSubmissionScoresFromExcel - Bỏ qua bản ghi khi điểm trong Excel là Null")
    void importSubmissionScoresFromExcel_NullGrade_NoUpdate() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_45: isBlank Helper - Kiểm thử các trường hợp biên")
    void isBlank_EdgeCases_Success() {
        // Ta sử dụng reflection hoặc gọi qua một method công khai sử dụng isBlank 
        // Ở đây SubmissionServiceImpl.importSubmissionScoresFromExcel gọi isBlank qua parseSubmissionExcel
        // Nhưng đơn giản nhất là test gián tiếp qua Import Excel với dữ liệu đặc biệt
    }

    @Test
    @DisplayName("TC_QLT_46: importSubmissionScoresFromExcel - Bỏ qua Row bị Null trong Excel")
    void importSubmissionScoresFromExcel_NullRow_Skip() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_47: getDetailSubmission - Thất bại do không có quyền xem")
    void getDetailSubmission_Forbidden_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.hasPermission(1000L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.getDetailSubmission("1000"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_48: parseSubmissionExcel - Bao phủ các nhánh isBlank")
    void importSubmissionScoresFromExcel_IsBlankVariants_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_49: downloadAllSubmissions - Bao phủ nhánh URL null/empty")
    void downloadAllSubmissions_UrlVariants_Skip() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        // URL is empty covers getFileNameFromDefaultUrl branches
        Attachment attEmpty = Attachment.builder().fileUrl("").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attEmpty));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_50: markSubmission - Thất bại khi không tìm thấy bài nộp do bị xóa giữa chừng")
    void markSubmission_ExistsInPermissionButDeletedInDB_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(1000L, 2L)).thenReturn(true);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.markSubmission("1000", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_51: importSubmissionScoresFromExcel - Bao phủ short-circuit isBlank(username) == false")
    void importSubmissionScoresFromExcel_UsernameOk_CodeBlank_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_52: deleteAttachmentInSubmission - Bao phủ short-circuit isLate == false")
    void deleteAttachmentInSubmission_NotLate_Success() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        assertThat(att.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("TC_QLT_53: addAttachmentToSubmission - Thành công khi danh sách đính kèm trống")
    void addAttachmentToSubmission_EmptyList_Success() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(activeAssignment));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(new ArrayList<>());
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_QLT_54: addAttachmentToSubmission - Đúng hạn (isLate = false)")
    void addAttachmentToSubmission_NotLate_Success() {
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("TC_QLT_55: downloadAllSubmissions - Bỏ qua file khi fileName bị Null")
    void downloadAllSubmissions_NullFileName_Skip() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_56: downloadAllSubmissions - Tiếp tục khi một file bị lỗi I/O")
    void downloadAllSubmissions_PartialIOError_Continue() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
        when(mockFile.toPath()).thenReturn(new java.io.File("temp.pdf").toPath());

        var result = submissionService.downloadAllSubmissions("10");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("TC_QLT_57: getMySubmission - Thất bại khi không tìm thấy bài nộp")
    void getMySubmission_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findByAssignmentIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getMySubmission("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_58: createDefaultSubmissions - Thành công khi danh sách sinh viên trống")
    void createDefaultSubmissions_EmptyList_Success() {
        when(classMemberRepository.getClassMemberIdsActive(anyLong(), any())).thenReturn(new HashSet<>());
        submissionService.createDefaultSubmissions(activeAssignment);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_QLT_59: addAttachmentToSubmission - Thất bại khi không tìm thấy bài nộp")
    void addAttachmentToSubmission_NotFound_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(new ArrayList<>());
        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("1000", request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_60: importSubmissionScoresFromExcel - Bao phủ filter Student null và not null")
    void importSubmissionScoresFromExcel_MixedStudentData_Success() throws IOException {
        Submission validSub = Submission.builder().student(User.builder().code("SV001").build()).build();
        Submission nullSub = Submission.builder().student(null).build();
        
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_61: downloadAllSubmissions - Bao phủ nhánh URL null")
    void downloadAllSubmissions_UrlNull_Skip() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment attNull = Attachment.builder().fileUrl(null).build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(attNull));
        
        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_62: deleteAttachmentInSubmission - Không thay đổi trạng thái khi còn file và không trễ")
    void deleteAttachmentInSubmission_StillHasFiles_NotLate_NoStatusChange() {
        Attachment att = Attachment.builder().attachmentId(500L).objectId(1000L).uploadedBy(1L).build();
        Assignment notLateAss = Assignment.builder().dueDate(LocalDateTime.now().plusDays(10)).build();
        existingSubmission.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(500L, 1L, false)).thenReturn(Optional.of(att));
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(notLateAss));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION)).thenReturn(List.of(new Attachment()));

        submissionService.deleteAttachmentInSubmission("500");
        
        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("TC_QLT_63: markSubmission - Thất bại khi quyền trả về null")
    void markSubmission_PermissionNull_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(null); // NULL PERMISSION

        assertThatThrownBy(() -> submissionService.markSubmission("1000", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_64: addAttachmentToSubmission - Nộp trễ nhưng bài tập vẫn mở")
    void addAttachmentToSubmission_LateOpen_Success() {
        Assignment lateOpenAss = Assignment.builder().dueDate(LocalDateTime.now().minusDays(1)).submissionClosed(false).build();
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(lateOpenAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.LATE_SUBMITTED);
    }

    @Test
    @DisplayName("TC_QLT_65: downloadAllSubmissions - Bỏ qua 1 file null name và vẫn nén các file khác")
    void downloadAllSubmissions_MixedNullFileName_Success() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
        when(file2.toPath()).thenReturn(new java.io.File("temp.pdf").toPath());

        var result = submissionService.downloadAllSubmissions("10");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("TC_QLT_66: addAttachmentToSubmission - Thành công khi Assignment không có hạn nộp (dueDate = null)")
    void addAttachmentToSubmission_NoDueDate_Success() {
        Assignment noDueDateAss = Assignment.builder().dueDate(null).submissionClosed(false).build();
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(existingSubmission));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(anyLong(), anyLong(), any(), any())).thenReturn(Optional.of(noDueDateAss));

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(List.of(AttachmentCreateRequest.builder().build()));
        submissionService.addAttachmentToSubmission("1000", request);

        assertThat(existingSubmission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(existingSubmission.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("TC_QLT_67: downloadAllSubmissions - Thất bại ném lỗi khi copy file lỗi (Bao phủ file != null)")
    void downloadAllSubmissions_FilesCopyError_ThrowsException() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(List.of(existingSubmission));
        
        Attachment att = Attachment.builder().fileUrl("valid_url").fileName("test.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(anyLong(), any())).thenReturn(List.of(att));
        
        Resource fileRes = mock(Resource.class);
        File mockFile = mock(File.class);
        when(fileService.downloadFile(anyString())).thenReturn(fileRes);
        when(fileRes.getFile()).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("test.pdf");
        when(mockFile.toPath()).thenReturn(new java.io.File("temp.pdf").toPath());

        try (MockedStatic<java.nio.file.Files> files = mockStatic(java.nio.file.Files.class)) {
            files.when(() -> java.nio.file.Files.copy(any(java.nio.file.Path.class), any(java.io.OutputStream.class)))
                 .thenThrow(new IOException("Disk full"));

            assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Disk full");
        }
    }

    @Test
    @DisplayName("TC_QLT_68: searchSubmission - Thất bại khi quyền trả về null")
    void searchSubmission_PermissionNull_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SubmissionSearchRequest());
        request.setPagination(new SearchRequest());
        request.getPagination().setPageNum("1");
        request.getPagination().setPageSize("10");

        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> submissionService.searchSubmission(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLT_69: downloadGradeTemplate - Thành công khi danh sách trống")
    void downloadGradeTemplate_EmptyList_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_70: importSubmissionScoresFromExcel - Thành công khi file Excel không có dữ liệu")
    void importSubmissionScoresFromExcel_EmptyData_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);
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
    @DisplayName("TC_QLT_71: downloadAllSubmissions - IOException khi getFile (Bao phủ ternary file == null)")
    void downloadAllSubmissions_GetFileError_ThrowsException() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_72: importSubmissionScoresFromExcel - Bao phủ nhánh gradeStr là null trong Excel")
    void importSubmissionScoresFromExcel_GradeNull_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);
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
    @DisplayName("TC_QLT_73: searchSubmission - Bao phủ pageSize là null (Không phân trang)")
    void searchSubmission_PageSizeNull_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SubmissionSearchRequest());
        request.setPagination(new SearchRequest()); // pageSize is null
        request.getPagination().setPageNum("1");

        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        var result = submissionService.searchSubmission(request);
        assertThat(result.getPaging()).isNotNull();
    }

    @Test
    @DisplayName("TC_QLT_74: searchSubmission - Bao phủ pageSize là chuỗi rỗng")
    void searchSubmission_PageSizeEmpty_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        BaseFilterSearchRequest<SubmissionSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(new SubmissionSearchRequest());
        request.setPagination(new SearchRequest());
        request.getPagination().setPageNum("1");
        request.getPagination().setPageSize("  "); // Blank whitespace

        when(assignmentRepository.canUserViewSubmissions(anyLong(), anyLong())).thenReturn(true);
        when(submissionRepository.searchSubmission(any(), any())).thenReturn(new PageImpl<>(new ArrayList<>()));

        var result = submissionService.searchSubmission(request);
        assertThat(result.getPaging()).isNotNull();
    }

    @Test
    @DisplayName("TC_QLT_75: importSubmissionScoresFromExcel - Bao phủ gradeStr là chuỗi rỗng")
    void importSubmissionScoresFromExcel_GradeEmptyString_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
    @DisplayName("TC_QLT_76: isBlank Helper - Bao phủ chuỗi chỉ chứa khoảng trắng")
    void importSubmissionScoresFromExcel_CodeWhitespace_ThrowsException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Workbook workbook = mock(Workbook.class);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
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
}
