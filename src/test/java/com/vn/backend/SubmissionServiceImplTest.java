package com.vn.backend;

import com.vn.backend.dto.request.attachment.AttachmentCreateRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.request.submission.SubmissionSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionUpdateRequest;
import com.vn.backend.dto.response.common.PagingMeta;
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
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.FileService;
import com.vn.backend.services.impl.SubmissionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubmissionServiceImpl - Full Branch Coverage Test Suite")
class SubmissionServiceImplTest {

    // ──────────────────────── Mocks ────────────────────────
    @Mock private AuthService            authService;
    @Mock private SubmissionRepository   submissionRepository;
    @Mock private AttachmentRepository   attachmentRepository;
    @Mock private AssignmentRepository   assignmentRepository;
    @Mock private ClassMemberRepository  classMemberRepository;
    @Mock private FileService            fileService;
    @Mock private MessageUtils           messageUtils;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    // ──────────────────────── Fixtures ────────────────────────
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

    @Test
    @DisplayName("TC_QLBT_SSV1_001 – Không có permission → throw FORBIDDEN")
    void getDetailSubmission_NoPermission() {
        // hasPermission trả false → phải throw trước khi query submission
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.hasPermission(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.getDetailSubmission("100"))
                .isInstanceOf(AppException.class);

        verify(submissionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("TC_QLBT_SSV1_002 – Có permission nhưng submission không tồn tại → throw NOT_FOUND")
    void getDetailSubmission_SubmissionNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(999L, 2L)).thenReturn(true);
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getDetailSubmission("999"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV1_003 – Tìm thấy submission có 2 attachment → response.attachmentList.size = 2")
    void getDetailSubmission_FoundWithAttachments() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(100L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(100L, 2L)).thenReturn(true);

        Submission sub = buildSubmission(100L, 10L);
        sub.setStudent(studentUser);
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(sub));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                .thenReturn(Collections.emptyList());

        SubmissionDetailResponse resp = submissionService.getDetailSubmission("100");

        assertThat(resp.getAttachmentResponseList()).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 2 – getMySubmission  (3 TC)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC_QLBT_SSV2_001 – Không tìm thấy submission → trả về empty builder response (không throw)")
    void getMySubmission_NotFound_ReturnsEmpty() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
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
        when(authService.getCurrentUser()).thenReturn(studentUser);
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
        when(authService.getCurrentUser()).thenReturn(studentUser);
        Submission sub = buildSubmission(100L, 10L);
        sub.setStudent(studentUser);  // FIX: fromEntity() gọi entity.getStudent() → cần set student
        when(submissionRepository.findByAssignmentIdAndStudentId(10L, 1L))
                .thenReturn(Optional.of(sub));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(100L, AttachmentType.SUBMISSION))
                .thenReturn(Collections.emptyList());

        SubmissionDetailResponse resp = submissionService.getMySubmission("10");

        assertThat(resp.getAttachmentResponseList()).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 3 – createDefaultSubmissions  (3 TC)
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 4 – deleteAttachmentInSubmission  (9 TC)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC_QLBT_SSV4_001 – Attachment không tìm thấy hoặc không thuộc user → throw NOT_FOUND")
    void deleteAttachment_AttachmentNotFound() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(99L, 1L, false))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.deleteAttachmentInSubmission("99"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV4_002 – Attachment tìm thấy nhưng submission không tồn tại → throw NOT_FOUND")
    void deleteAttachment_SubmissionNotFound() {
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);

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

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 5 – addAttachmentToSubmission  (5 TC)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC_QLBT_SSV5_001 – Submission không tìm thấy → throw NOT_FOUND")
    void addAttachment_SubmissionNotFound() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.findBySubmissionIdAndStudentId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("999", new SubmissionUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV5_002 – Assignment không còn cho phép nộp → throw NOT_FOUND")
    void addAttachment_AssignmentNotEligible() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        Submission sub = buildSubmission(100L, 10L);
        when(submissionRepository.findBySubmissionIdAndStudentId(100L, 1L))
                .thenReturn(Optional.of(sub));
        when(assignmentRepository.findAssignmentIfUserCanSubmit(
                1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("100", new SubmissionUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV5_003 – Nộp trễ + submissionClosed=true → throw LATE_SUBMISSION_NOT_ALLOWED")
    void addAttachment_LateAndClosed() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
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

        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("100", new SubmissionUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV5_004 – Đúng hạn + 1 file → status = SUBMITTED, attachment được lưu")
    void addAttachment_OnTime_StatusSubmitted() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
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
        when(authService.getCurrentUser()).thenReturn(studentUser);
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

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 6 – searchSubmission  (3 TC)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC_QLBT_SSV6_001 – canUserViewSubmissions = false → throw FORBIDDEN")
    void searchSubmission_NoPermission() {
        when(authService.getCurrentUser()).thenReturn(studentUser);

        SubmissionSearchRequest filter = new SubmissionSearchRequest();
        filter.setAssignmentId("10");
        BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(new SearchRequest());

        when(assignmentRepository.canUserViewSubmissions(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.searchSubmission(req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV6_002 – Có quyền, có kết quả → response.content.size = 2")
    void searchSubmission_WithResults() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);

        SubmissionSearchRequest filter = new SubmissionSearchRequest();
        filter.setAssignmentId("10");
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("0");
        pagination.setPageSize("10");
        BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(pagination);

        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);

        SubmissionSearchRequest filter = new SubmissionSearchRequest();
        filter.setAssignmentId("10");
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("0");
        pagination.setPageSize("10");
        BaseFilterSearchRequest<SubmissionSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(pagination);

        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);
        when(submissionRepository.searchSubmission(any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseListData<SubmissionSearchResponse> result = submissionService.searchSubmission(req);

        assertThat(result.getContent()).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GROUP 7 – markSubmission  (5 TC)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("TC_QLBT_SSV7_001 – hasPermission = false → throw FORBIDDEN")
    void markSubmission_NoPermission() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(submissionRepository.hasPermission(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.markSubmission("100", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);

        verify(submissionRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("TC_QLBT_SSV7_002 – Có permission nhưng submission không tồn tại → throw NOT_FOUND")
    void markSubmission_SubmissionNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(999L, 2L)).thenReturn(true);
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.markSubmission("999", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV7_003 – Chấm điểm hợp lệ (8.5/10) → gradingStatus = GRADED, gradedAt được set")
    void markSubmission_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(100L, 2L)).thenReturn(true);

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
    @DisplayName("TC_QLBT_SSV7_004 – Điểm âm → phải throw AppException (BUG: backend không validate)")
    void markSubmission_NegativeGrade_ShouldThrow() {
        // Kỳ vọng thực tế: điểm không thể < 0
        // BUG DETECTED: @AllowFormat(NON_NEGATIVE_DECIMAL_2) chỉ được enforce ở Controller layer,
        //               Service layer không validate → grade âm vẫn được lưu vào DB
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(100L, 2L)).thenReturn(true);

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
    @DisplayName("TC_QLBT_SSV7_005 – Điểm vượt maxScore (12/10) → phải throw AppException (BUG: backend không validate)")
    void markSubmission_GradeExceedsMaxScore_ShouldThrow() {
        // Kỳ vọng thực tế: grade không được vượt quá maxScore của Assignment
        // BUG DETECTED: Service không load Assignment để check maxScore → grade=12 với maxScore=10 vẫn được lưu
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(100L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV8_002 – Không có submission nào → throw NOT_FOUND")
    void downloadAllSubmissions_NoSubmissions() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);
        when(submissionRepository.findAllByAssignmentId(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> submissionService.downloadAllSubmissions("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV8_003 – Có submission nhưng tất cả attachment URL = null (fileName=null) → throw NOT_FOUND")
    void downloadAllSubmissions_AllAttachmentsHaveNullFileName() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.downloadGradeTemplate("10"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV9_002 – data rỗng → vẫn trả về ByteArrayResource (excel rỗng, không throw)")
    void downloadGradeTemplate_EmptyData() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);
        when(submissionRepository.findAllForExcel(10L)).thenReturn(Collections.emptyList());

        // ExcelUtils.exportToExcel() là static, không mock được → chỉ verify không throw
        ByteArrayResource resource = submissionService.downloadGradeTemplate("10");

        assertThat(resource).isNotNull();
    }

    @Test
    @DisplayName("TC_QLBT_SSV9_003 – Có data → trả về ByteArrayResource (không throw)")
    void downloadGradeTemplate_WithData() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", mock(MockMultipartFile.class)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV10_002 – File không phải Excel hợp lệ → IOException → throw FILE_UPLOAD_FAILED")
    void importScores_InvalidFile() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

        MockMultipartFile file = buildExcelFile(List.<Object[]>of(
                new Object[]{"1", "user1", "Nguyen Van A", "B21DCCN001", null, null, null, "abc"}
        ));

        assertThatThrownBy(() -> submissionService.importSubmissionScoresFromExcel("10", file))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_SSV10_007 – Mã SV trong file không có trong hệ thống → throw IMPORT_SUBMISSION_NOT_FOUND")
    void importScores_StudentCodeNotFound() throws IOException {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
    @DisplayName("TC_QLBT_SSV10_009 – Điểm import vượt maxScore → phải throw AppException (BUG: backend không validate)")
    void importScores_GradeExceedsMaxScore_ShouldThrow() throws IOException {
        // Kỳ vọng thực tế: điểm import không được vượt quá maxScore của Assignment
        // BUG DETECTED: parseSubmissionExcel() không nhận biết maxScore, chỉ parse số
        //               → Điểm 99.0 với maxScore=10 vẫn được lưu vào DB
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.canUserViewSubmissions(10L, 2L)).thenReturn(true);

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
