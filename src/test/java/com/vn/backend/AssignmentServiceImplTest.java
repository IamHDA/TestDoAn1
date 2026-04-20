package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.assignment.*;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.response.assignment.*;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.*;
import com.vn.backend.services.impl.AssignmentServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentServiceImpl - Bộ kiểm thử đầy đủ")
class AssignmentServiceImplTest {

    // ===== Mock các dependency =====
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AuthService authService;
    @Mock private MessageUtils messageUtils;
    @Mock private AnnouncementService announcementService;
    @Mock private SubmissionService submissionService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    // ===== Dữ liệu mẫu dùng chung =====
    private User teacherUser;
    private User assistantUser;
    private User studentUser;
    private User otherUser;
    private Classroom classroom;
    private Assignment sampleAssignment;
    // User giả lập cho quan hệ createdByUser (LAZY)
    private User creatorUser;

    @BeforeEach
    void setUp() {
        // Khởi tạo người dùng mẫu
        teacherUser   = User.builder().id(1L).fullName("Giảng viên").email("teacher@test.com").role(Role.TEACHER).build();
        assistantUser = User.builder().id(4L).fullName("Trợ giảng").email("assistant@test.com").role(Role.TEACHER).build();
        studentUser   = User.builder().id(2L).fullName("Sinh viên").email("student@test.com").role(Role.STUDENT).build();
        otherUser     = User.builder().id(3L).fullName("Ngoài lớp").email("other@test.com").role(Role.STUDENT).build();

        // User tạo bài tập (dùng cho quan hệ @ManyToOne FetchType.LAZY)
        creatorUser = User.builder().id(1L).fullName("Giảng viên").email("teacher@test.com")
                .avatarUrl("http://avatar.com/teacher.jpg").build();

        // Lớp học mẫu
        classroom = Classroom.builder()
                .classroomId(100L)
                .teacherId(1L)
                .className("Lớp CNTT-01")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .isActive(true)
                .build();

        // Bài tập mẫu - gán createdByUser để tránh NPE trong mapToResponse()
        sampleAssignment = Assignment.builder()
                .assignmentId(50L)
                .classroomId(100L)
                .title("Bài tập mẫu")
                .content("Làm bài này")
                .maxScore(10L)
                .dueDate(LocalDateTime.now().plusDays(3))
                .submissionClosed(false)
                .isDeleted(false)
                .createdBy(1L)
                .build();
        // Gán thủ công vì Builder Lombok không kế thừa field BaseEntity
        sampleAssignment.setCreatedAt(LocalDateTime.now().minusDays(5));
        sampleAssignment.setUpdatedAt(LocalDateTime.now().minusDays(1));
        // Gán lazy relationship để tránh NPE khi mapToResponse() gọi getCreatedByUser()
        sampleAssignment.setCreatedByUser(creatorUser);
    }

    // ==================== Helper methods ====================

    /** Tạo announcement mẫu */
    private Announcement mockAnnouncement() {
        Announcement ann = new Announcement();
        ann.setAnnouncementId(200L);
        ann.setObjectId(50L);
        ann.setType(AnnouncementType.ASSIGNMENT);
        ann.setAllowComments(true);
        ann.setIsDeleted(false);
        return ann;
    }

    /** Mock quyền giảng viên cho lớp */
    private void mockTeacherPermission(Long classroomId, Long userId) {
        when(classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)).thenReturn(true);
    }

    /**
     * Mock quyền trợ giảng:
     * - Không phải teacher của lớp
     * - Nhưng là ASSISTANT trong classMember
     * - Cần mock cả findByClassroomIdAndUserId để validateClassroomAccess không fail
     */
    private void mockAssistantPermission(Long classroomId, Long userId) {
        when(classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)).thenReturn(false);
        // Mock cho validateClassroomAccess -> findByClassroomIdAndUserId
        ClassMember assistantMember = ClassMember.builder()
                .userId(userId)
                .memberRole(ClassMemberRole.ASSISTANT)
                .memberStatus(ClassMemberStatus.ACTIVE)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(eq(classroomId), eq(userId)))
                .thenReturn(Optional.of(assistantMember));
        // Mock cho validateEditPermission -> findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                eq(classroomId), eq(userId), eq(ClassMemberRole.ASSISTANT), eq(ClassMemberStatus.ACTIVE)))
                .thenReturn(Optional.of(assistantMember));
    }

    /**
     * Mock quyền sinh viên truy cập lớp:
     * - Không phải teacher
     * - Là STUDENT trong classMember với status ACTIVE
     */
    private void mockStudentAccess(Long classroomId, Long userId) {
        when(classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)).thenReturn(false);
        ClassMember studentMember = ClassMember.builder()
                .userId(userId)
                .memberRole(ClassMemberRole.STUDENT)
                .memberStatus(ClassMemberStatus.ACTIVE)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(eq(classroomId), eq(userId)))
                .thenReturn(Optional.of(studentMember));
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                eq(classroomId), eq(userId), eq(ClassMemberStatus.ACTIVE), eq(ClassMemberRole.STUDENT)))
                .thenReturn(true);
    }

    /**
     * Tạo Assignment với createdAt đã set (tránh NPE trong getImprovementTrend vì service gọi
     * assignment.getCreatedAt().format(...) khi group theo SESSION/WEEK/MONTH)
     */
    private Assignment buildAssignmentWithTimestamp(Long id, Long classroomId, String title, LocalDateTime dueDate) {
        Assignment a = Assignment.builder()
                .assignmentId(id)
                .classroomId(classroomId)
                .title(title)
                .dueDate(dueDate)
                .isDeleted(false)
                .createdBy(1L)
                .build();
        // Gán createdAt vì đây là field của BaseEntity, Hibernate quản lý, Builder không set được
        a.setCreatedAt(dueDate.minusDays(5));
        a.setUpdatedAt(dueDate.minusDays(1));
        return a;
    }

    // =================================================================
    //  NHÓM 1: TẠO BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 1: Tạo bài tập (createAssignment)")
    class CreateAssignmentTests {

        @Test
        @DisplayName("TC_ASS1_001: Giảng viên tạo bài tập thành công")
        void createAssignment_HappyPath_Teacher() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập mới");
            req.setContent("Chi tiết");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(2));
            req.setSubmissionClosed(false);
            req.setAllowComments(true);

            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> {
                Assignment a = inv.getArgument(0);
                a.setAssignmentId(50L);
                return a;
            });

            Long id = assignmentService.createAssignment(100L, req);
            assertThat(id).isEqualTo(50L);
            // Kiểm tra các side effect
            verify(announcementRepository, times(1)).save(any(Announcement.class));
            verify(submissionService, times(1)).createDefaultSubmissions(any(Assignment.class));
            verify(announcementService, times(1)).notifyAnnouncement(any(Announcement.class));
        }

        @Test
        @DisplayName("TC_ASS1_002: Trợ giảng tạo bài tập thành công")
        void createAssignment_HappyPath_Assistant() {
            when(authService.getCurrentUser()).thenReturn(assistantUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockAssistantPermission(100L, 4L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập của trợ giảng");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(2));

            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> {
                Assignment a = inv.getArgument(0);
                a.setAssignmentId(51L);
                return a;
            });

            Long id = assignmentService.createAssignment(100L, req);
            assertThat(id).isEqualTo(51L);
        }

        @Test
        @DisplayName("TC_ASS1_003: Lớp học không tồn tại → AppException BAD_REQUEST")
        void createAssignment_ClassroomNotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(99L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            assertThatThrownBy(() -> assignmentService.createAssignment(99L, req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("TC_ASS1_004: Sinh viên không có quyền tạo bài → FORBIDDEN")
        void createAssignment_Forbidden() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
            when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                    eq(100L), eq(2L), eq(ClassMemberRole.ASSISTANT), eq(ClassMemberStatus.ACTIVE)))
                    .thenReturn(Optional.empty());

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("TC_ASS1_005: Tạo bài tập kèm file đính kèm → lưu attachment")
        void createAssignment_WithAttachments() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập có file");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(1));

            List<AnnouncementCreateRequest.AttachmentRequest> attachments = new ArrayList<>();
            AnnouncementCreateRequest.AttachmentRequest att1 = new AnnouncementCreateRequest.AttachmentRequest();
            att1.setFileName("file1.pdf");
            att1.setFileUrl("url1");
            att1.setDescription("mô tả file");
            attachments.add(att1);
            req.setAttachments(attachments);

            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> {
                Assignment a = inv.getArgument(0);
                a.setAssignmentId(52L);
                return a;
            });

            assignmentService.createAssignment(100L, req);
            // Phải lưu attachment
            verify(attachmentRepository, times(1)).saveAllAndFlush(anyList());
        }

        @Test
        @DisplayName("TC_ASS1_006: maxScore = 0 phải bị từ chối (Kỳ vọng ném AppException)")
        void createAssignment_ZeroMaxScore_ShouldFail() {
                        when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập điểm 0");
            req.setMaxScore(0L);
            req.setDueDate(LocalDateTime.now().plusDays(1));

            // Kỳ vọng nghiệp vụ: phải throw AppException (bug: code không throw)
            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS1_007: Tiêu đề null phải bị từ chối (Kỳ vọng ném AppException)")
        void createAssignment_NullTitle_ShouldFail() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle(null); // Tiêu đề bỏ trống
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS1_008: Hạn nộp trong quá khứ phải bị từ chối")
        void createAssignment_PastDueDate_ShouldFail() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập quá hạn");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().minusDays(1)); // Hạn đã qua

            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS1_009: Tạo bài tập với submissionClosed = true → lưu đúng trạng thái")
        void createAssignment_SubmissionClosedTrue() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài đóng nộp");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(1));
            req.setSubmissionClosed(true);

            ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
            when(assignmentRepository.saveAndFlush(captor.capture())).thenAnswer(inv -> {
                Assignment a = inv.getArgument(0);
                a.setAssignmentId(55L);
                return a;
            });

            assignmentService.createAssignment(100L, req);
            // Kiểm tra flag được lưu đúng
            assertThat(captor.getValue().isSubmissionClosed()).isTrue();
        }

        @Test
        @DisplayName("TC_ASS1_010: Bài tập không có file đính kèm → không gọi saveAllAndFlush")
        void createAssignment_NoAttachments_NoSave() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài không file");
            req.setMaxScore(10L);
            req.setDueDate(LocalDateTime.now().plusDays(1));
            req.setAttachments(null); // Không có file

            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> {
                Assignment a = inv.getArgument(0);
                a.setAssignmentId(56L);
                return a;
            });

            assignmentService.createAssignment(100L, req);
            // Không được gọi saveAllAndFlush vì không có attachment
            verify(attachmentRepository, never()).saveAllAndFlush(anyList());
        }

        @Test
        @DisplayName("TC_ASS1_011: Người dùng bên ngoài lớp (không phải member) → FORBIDDEN")
        void createAssignment_UserNotInClassroom_Forbidden() {
            when(authService.getCurrentUser()).thenReturn(otherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            // otherUser không phải teacher
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
            // Không phải assistant
            when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                    eq(100L), eq(3L), eq(ClassMemberRole.ASSISTANT), eq(ClassMemberStatus.ACTIVE)))
                    .thenReturn(Optional.empty());

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("TC_ASS1_012: Điểm tối đa âm phải bị từ chối (Kỳ vọng ném AppException)")
        void createAssignment_NegativeMaxScore_ShouldFail() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            mockTeacherPermission(100L, 1L);

            AssignmentCreateRequest req = new AssignmentCreateRequest();
            req.setTitle("Bài tập điểm âm");
            req.setMaxScore(-5L);

            assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                    .isInstanceOf(AppException.class);
        }
    }

    // =================================================================
    //  NHÓM 2: CẬP NHẬT BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 2: Cập nhật bài tập (updateAssignment)")
    class UpdateAssignmentTests {

        @Test
        @DisplayName("TC_ASS2_001: Cập nhật tiêu đề và nội dung thành công")
        void updateAssignment_HappyPath() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setTitle("Tiêu đề mới");
            req.setContent("Nội dung mới");
            req.setDueDate(LocalDateTime.now().plusDays(5));

            assignmentService.updateAssignment(50L, req);
            assertThat(sampleAssignment.getTitle()).isEqualTo("Tiêu đề mới");
            assertThat(sampleAssignment.getContent()).isEqualTo("Nội dung mới");
            verify(assignmentRepository).save(sampleAssignment);
        }

        @Test
        @DisplayName("TC_ASS2_002: Bài tập không tồn tại → AppException")
        void updateAssignment_NotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.updateAssignment(999L, new AssignmentUpdateRequest()))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS2_003: Sinh viên không có quyền sửa → FORBIDDEN")
        void updateAssignment_Forbidden_Student() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
            when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                    eq(100L), eq(2L), eq(ClassMemberRole.ASSISTANT), eq(ClassMemberStatus.ACTIVE)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.updateAssignment(50L, new AssignmentUpdateRequest()))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS2_004: Cập nhật maxScore và dueDate")
        void updateAssignment_MaxScoreAndDueDate() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setMaxScore(20L);
            req.setDueDate(LocalDateTime.now().plusDays(10));

            assignmentService.updateAssignment(50L, req);
            assertThat(sampleAssignment.getMaxScore()).isEqualTo(20L);
            assertThat(sampleAssignment.getDueDate()).isAfter(LocalDateTime.now().plusDays(9));
        }

        @Test
        @DisplayName("TC_ASS2_005: Cập nhật allowComments → thay đổi trong announcement")
        void updateAssignment_AllowComments() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            Announcement ann = mockAnnouncement();
            ann.setAllowComments(false);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT)).thenReturn(ann);

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setAllowComments(true);
            req.setDueDate(LocalDateTime.now().plusDays(1));

            assignmentService.updateAssignment(50L, req);
            assertThat(ann.getAllowComments()).isTrue();
            verify(announcementRepository).save(ann);
        }

        @Test
        @DisplayName("TC_ASS2_006: Cập nhật attachment - giữ cũ + thêm mới + xóa attachment không còn trong list")
        void updateAssignment_AttachmentsComplex() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            // Attachment cũ đang tồn tại trong DB
            Attachment existingAtt = Attachment.builder()
                    .attachmentId(101L).objectId(50L)
                    .attachmentType(AttachmentType.ASSIGNMENT)
                    .fileName("cu.pdf").isDeleted(false).build();
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(List.of(existingAtt));

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setDueDate(LocalDateTime.now().plusDays(1));
            List<AssignmentUpdateRequest.AttachmentRequest> attReqList = new ArrayList<>();

            // Giữ attachment cũ
            AssignmentUpdateRequest.AttachmentRequest keep = new AssignmentUpdateRequest.AttachmentRequest();
            keep.setAttachmentId(101L);
            keep.setFileName("cu.pdf");
            attReqList.add(keep);

            // Thêm attachment mới
            AssignmentUpdateRequest.AttachmentRequest newAtt = new AssignmentUpdateRequest.AttachmentRequest();
            newAtt.setFileName("moi.pdf");
            newAtt.setFileUrl("url-moi");
            attReqList.add(newAtt);

            req.setAttachments(attReqList);

            assignmentService.updateAssignment(50L, req);
            // Attachment mới phải được lưu
            verify(attachmentRepository, times(1)).save(any(Attachment.class));
            // Attachment cũ được giữ, không bị xóa mềm
            verify(attachmentRepository, never()).softDeleteById(101L);
        }

        @Test
        @DisplayName("TC_ASS2_007: Xóa hết attachment khi truyền danh sách rỗng")
        void updateAttachment_RemoveAll() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            Attachment existing = Attachment.builder().attachmentId(102L).objectId(50L).isDeleted(false).build();
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(List.of(existing));

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setAttachments(Collections.emptyList()); // Xóa hết

            assignmentService.updateAssignment(50L, req);
            verify(attachmentRepository).softDeleteById(102L);
        }

        @Test
        @DisplayName("TC_ASS2_008: Giảm maxScore dưới điểm đã chấm → phải từ chối (Kỳ vọng ném AppException)")
        void updateAssignment_LowerMaxScore_ValidationMissing() {
            // Quy tắc nghiệp vụ: không được hạ maxScore xuống dưới điểm đã có → hiện tại thiếu validation
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setMaxScore(1L); // maxScore cũ là 10, sinh viên có thể đã đạt 9
            req.setDueDate(LocalDateTime.now().plusDays(1));

            assertThatThrownBy(() -> assignmentService.updateAssignment(50L, req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS2_009: Đặt hạn nộp trong quá khứ → phải từ chối (Kỳ vọng ném AppException)")
        void updateAssignment_PastDueDate_ShouldFail() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setDueDate(LocalDateTime.now().minusDays(1)); // Hạn đã qua

            assertThatThrownBy(() -> assignmentService.updateAssignment(50L, req))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS2_010: Trợ giảng có quyền cập nhật bài tập")
        void updateAssignment_AssistantSuccess() {
            when(authService.getCurrentUser()).thenReturn(assistantUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockAssistantPermission(100L, 4L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setTitle("Tiêu đề sửa bởi trợ giảng");
            req.setDueDate(LocalDateTime.now().plusDays(3));

            assignmentService.updateAssignment(50L, req);
            assertThat(sampleAssignment.getTitle()).isEqualTo("Tiêu đề sửa bởi trợ giảng");
        }

        @Test
        @DisplayName("TC_ASS2_011: Cập nhật submissionClosed → lưu đúng trạng thái")
        void updateAssignment_SubmissionClosed() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            req.setSubmissionClosed(true);

            assignmentService.updateAssignment(50L, req);
            assertThat(sampleAssignment.isSubmissionClosed()).isTrue();
        }

        @Test
        @DisplayName("TC_ASS2_012: Danh sách file đính kèm chứa giá trị null ném AppException")
        void updateAssignment_NullAttachmentItem_ShouldFail() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            AssignmentUpdateRequest req = new AssignmentUpdateRequest();
            List<AssignmentUpdateRequest.AttachmentRequest> attReqList = new ArrayList<>();
            attReqList.add(null); // Gửi phần tử null
            req.setAttachments(attReqList);

            assertThatThrownBy(() -> assignmentService.updateAssignment(50L, req))
                    .isInstanceOf(AppException.class);
        }
    }

    // =================================================================
    //  NHÓM 3: XÓA MỀM BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 3: Xóa mềm bài tập (softDeleteAssignment)")
    class SoftDeleteAssignmentTests {

        @Test
        @DisplayName("TC_ASS3_001: Giảng viên xóa mềm bài tập thành công")
        void softDelete_HappyPath() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            Announcement ann = mockAnnouncement();
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT)).thenReturn(ann);

            assignmentService.softDeleteAssignment(50L);
            assertThat(sampleAssignment.isDeleted()).isTrue();
            assertThat(ann.getIsDeleted()).isTrue();
            verify(assignmentRepository).save(sampleAssignment);
            verify(announcementRepository).save(ann);
        }

        @Test
        @DisplayName("TC_ASS3_002: Bài tập không tồn tại → AppException")
        void softDelete_NotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.softDeleteAssignment(999L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS3_003: Sinh viên không có quyền xóa → FORBIDDEN")
        void softDelete_Forbidden_Student() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            // studentUser không phải teacher
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
            // validateClassroomAccess: studentUser là STUDENT, status ACTIVE → vượt qua
            ClassMember studentMember = ClassMember.builder()
                    .userId(2L).memberRole(ClassMemberRole.STUDENT).memberStatus(ClassMemberStatus.ACTIVE).build();
            when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(studentMember));
            // validateEditPermission: không phải ASSISTANT → throw
            when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                    eq(100L), eq(2L), eq(ClassMemberRole.ASSISTANT), eq(ClassMemberStatus.ACTIVE)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.softDeleteAssignment(50L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS3_004: Trợ giảng có quyền xóa bài tập")
        void softDelete_AssistantSuccess() {
            when(authService.getCurrentUser()).thenReturn(assistantUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            // mockAssistantPermission đã mock đủ cả validateClassroomAccess & validateEditPermission
            mockAssistantPermission(100L, 4L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            assignmentService.softDeleteAssignment(50L);
            assertThat(sampleAssignment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("TC_ASS3_005: Người dùng ngoài lớp → không vượt qua validateClassroomAccess")
        void softDelete_UserNotInClassroom() {
            when(authService.getCurrentUser()).thenReturn(otherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            // Không phải teacher
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
            // Không phải member
            when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.softDeleteAssignment(50L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS3_006: Thành viên không hoạt động (INACTIVE) → bị từ chối")
        void softDelete_InActiveMember() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
            // Thành viên bị INACTIVE
            ClassMember inactiveMember = ClassMember.builder()
                    .userId(2L).memberRole(ClassMemberRole.STUDENT).memberStatus(ClassMemberStatus.INACTIVE).build();
            when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L))
                    .thenReturn(Optional.of(inactiveMember));

            assertThatThrownBy(() -> assignmentService.softDeleteAssignment(50L))
                    .isInstanceOf(AppException.class);
        }
    }

    // =================================================================
    //  NHÓM 4: XEM CHI TIẾT BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 4: Xem chi tiết bài tập (getAssignmentDetail)")
    class GetAssignmentDetailTests {

        @Test
        @DisplayName("TC_ASS4_001: Giảng viên xem chi tiết → có quyền sửa và xóa")
        void getDetail_TeacherSuccess() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(Collections.emptyList());

            AssignmentResponse response = assignmentService.getAssignmentDetail(50L);
            assertThat(response.getCanEdit()).isTrue();
            assertThat(response.getCanDelete()).isTrue();
            // Giảng viên không được nộp bài
            assertThat(response.getCanSubmit()).isFalse();
        }

        @Test
        @DisplayName("TC_ASS4_002: Sinh viên xem chi tiết → có quyền nộp bài")
        void getDetail_StudentSuccess() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockStudentAccess(100L, 2L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(Collections.emptyList());

            AssignmentResponse response = assignmentService.getAssignmentDetail(50L);
            assertThat(response.getCanSubmit()).isTrue();
            assertThat(response.getCanEdit()).isFalse();
            assertThat(response.getCanDelete()).isFalse();
        }

        @Test
        @DisplayName("TC_ASS4_003: Sinh viên không thể nộp nếu đã hết hạn ")
        void getDetail_StudentCannotSubmitIfPastDue() {
            // Thiết lập hạn đã qua
            sampleAssignment.setDueDate(LocalDateTime.now().minusDays(1));
            sampleAssignment.setSubmissionClosed(false);

            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockStudentAccess(100L, 2L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(Collections.emptyList());

            AssignmentResponse response = assignmentService.getAssignmentDetail(50L);
            // Kỳ vọng: canSubmit = false khi đã hết hạn (BUG: code hiện tại vẫn trả true)
            assertThat(response.getCanSubmit()).isFalse();
        }

        @Test
        @DisplayName("TC_ASS4_004: Người dùng ngoài lớp → FORBIDDEN")
        void getDetail_Forbidden() {
            when(authService.getCurrentUser()).thenReturn(otherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
            when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.getAssignmentDetail(50L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS4_005: Bài tập không tồn tại → AppException")
        void getDetail_NotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.getAssignmentDetail(999L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS4_006: Bài tập có file đính kèm → kết quả gồm danh sách attachment")
        void getDetail_WithAttachments() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());

            Attachment att = Attachment.builder()
                    .attachmentId(201L).fileName("taiLieu.pdf")
                    .fileUrl("url").description("mô tả").build();
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(List.of(att));

            AssignmentResponse response = assignmentService.getAssignmentDetail(50L);
            assertThat(response.getAttachments()).hasSize(1);
            assertThat(response.getAttachments().get(0).getFileName()).isEqualTo("taiLieu.pdf");
        }

        @Test
        @DisplayName("TC_ASS4_007: Trợ giảng xem chi tiết → có quyền sửa và xóa")
        void getDetail_AssistantCanEdit() {
            when(authService.getCurrentUser()).thenReturn(assistantUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockAssistantPermission(100L, 4L);
            when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                    .thenReturn(mockAnnouncement());
            when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(50L, AttachmentType.ASSIGNMENT))
                    .thenReturn(Collections.emptyList());

            AssignmentResponse response = assignmentService.getAssignmentDetail(50L);
            assertThat(response.getCanEdit()).isTrue();
            assertThat(response.getCanDelete()).isTrue();
        }
    }

    // =================================================================
    //  NHÓM 5: DANH SÁCH BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 5: Danh sách bài tập (getAssignmentList)")
    class GetAssignmentListTests {

        @Test
        @DisplayName("TC_ASS5_001: Giảng viên lấy danh sách có phân trang")
        void getList_TeacherSuccess() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            mockTeacherPermission(100L, 1L);

            Assignment a1 = buildSimpleAssignment(50L, 100L);
            Assignment a2 = buildSimpleAssignment(51L, 100L);
            Page<Assignment> page = new PageImpl<>(List.of(a1, a2));
            when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any(Pageable.class)))
                    .thenReturn(page);

            AssignmentListRequest req = new AssignmentListRequest();
            req.setPagination(new SearchRequest());

            ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
            assertThat(result.getContent()).hasSize(2);
            // Kiểm tra paging metadata
            assertThat(result.getPaging().getTotalRows()).isEqualTo(2);
        }

        @Test
        @DisplayName("TC_ASS5_002: Sinh viên lấy danh sách bài tập")
        void getList_StudentSuccess() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            mockStudentAccess(100L, 2L);

            Page<Assignment> page = new PageImpl<>(List.of(sampleAssignment));
            when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any()))
                    .thenReturn(page);

            AssignmentListRequest req = new AssignmentListRequest();
            req.setPagination(new SearchRequest());

            ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("TC_ASS5_003: Danh sách trống → trả về collection rỗng")
        void getList_Empty() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            mockTeacherPermission(100L, 1L);
            when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any()))
                    .thenReturn(Page.empty());

            AssignmentListRequest req = new AssignmentListRequest();
            req.setPagination(new SearchRequest());

            ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS5_004: Người dùng ngoài lớp → FORBIDDEN")
        void getList_Forbidden() {
            when(authService.getCurrentUser()).thenReturn(otherUser);
            when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
            when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.empty());

            AssignmentListRequest req = new AssignmentListRequest();
            req.setPagination(new SearchRequest());

            assertThatThrownBy(() -> assignmentService.getAssignmentList(100L, req))
                    .isInstanceOf(AppException.class);
        }

        /** Tạo assignment đơn giản cho test danh sách (không cần createdAt/createdByUser) */
        private Assignment buildSimpleAssignment(Long id, Long classroomId) {
            return Assignment.builder()
                    .assignmentId(id)
                    .classroomId(classroomId)
                    .title("Bài tập " + id)
                    .dueDate(LocalDateTime.now().plusDays(2))
                    .isDeleted(false)
                    .build();
        }
    }

    // =================================================================
    //  NHÓM 6: THÊM NGƯỜI GIAO BÀI
    // =================================================================
    @Nested
    @DisplayName("Nhóm 6: Thêm người giao bài (addAssignee)")
    class AddAssigneeTests {

        @Test
        @DisplayName("TC_ASS6_001: Giảng viên giao bài cho sinh viên mới → tạo submission")
        void addAssignee_NewSubmission() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentId(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                    100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(true);
            when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L)).thenReturn(Optional.empty());

            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("2");

            assignmentService.addAssignee("50", request);
            // Phải tạo submission mới
            verify(submissionRepository).save(any(Submission.class));
        }

        @Test
        @DisplayName("TC_ASS6_002: Sinh viên đã được giao bài → không tạo thêm submission")
        void addAssignee_AlreadyExists() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentId(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                    100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(true);
            when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L))
                    .thenReturn(Optional.of(new Submission()));

            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("2");

            assignmentService.addAssignee("50", request);
            // Không được tạo thêm submission
            verify(submissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC_ASS6_003: Bài tập không tồn tại → AppException")
        void addAssignee_AssignmentNotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentId(99L)).thenReturn(Optional.empty());

            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("2");
            assertThatThrownBy(() -> assignmentService.addAssignee("99", request))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS6_004: ID sinh viên không phải thành viên lớp → AppException")
        void addAssignee_NotStudentInClass() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentId(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);
            when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                    100L, 3L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(false);

            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("3");

            assertThatThrownBy(() -> assignmentService.addAssignee("50", request))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS6_005: Trợ giảng có quyền giao bài")
        void addAssignee_AssistantSuccess() {
            when(authService.getCurrentUser()).thenReturn(assistantUser);
            when(assignmentRepository.findByAssignmentId(50L)).thenReturn(Optional.of(sampleAssignment));
            mockAssistantPermission(100L, 4L);
            when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                    100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(true);
            when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L)).thenReturn(Optional.empty());

            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("2");
            assignmentService.addAssignee("50", request);
            verify(submissionRepository).save(any());
        }

        @Test
        @DisplayName("TC_ASS6_006: Gửi assignmentId không phải số sẽ ném AppException")
        void addAssignee_InvalidAssignmentIdFormat_ShouldThrowBadRequest() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            
            AssigneeAddRequest request = new AssigneeAddRequest();
            request.setUserId("2");
            // Gọi với ID kiểu string "abc" -> Long.parseLong("abc") sẽ ném NumberFormatException, bỏ qua luồng AppException
            assertThatThrownBy(() -> assignmentService.addAssignee("abc_invalid", request))
                    .isInstanceOf(AppException.class);
        }
    }

    // =================================================================
    //  NHÓM 7: TÌM KIẾM NGƯỜI GIAO BÀI
    // =================================================================
    @Nested
    @DisplayName("Nhóm 7: Tìm kiếm assignee (searchAssignee)")
    class SearchAssigneeTests {

        @Test
        @DisplayName("TC_ASS7_001: Giảng viên tìm kiếm danh sách assignee thành công")
        void searchAssignee_Success() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);

            User stu = User.builder().id(2L).fullName("Sinh viên A").email("svA@test.com").build();
            AssigneeSearchQueryDTO dto = AssigneeSearchQueryDTO.builder()
                    .user(stu)
                    .isAssigned(true)
                    .build();

            Page<AssigneeSearchQueryDTO> page = new PageImpl<>(List.of(dto));
            when(submissionRepository.searchAssignee(any(AssigneeSearchRequestDTO.class), any(Pageable.class)))
                    .thenReturn(page);

            BaseFilterSearchRequest<AssigneeSearchRequest> request = new BaseFilterSearchRequest<>();
            request.setFilters(new AssigneeSearchRequest());
            request.getFilters().setAssignmentId("50");
            request.setPagination(new SearchRequest());

            ResponseListData<AssigneeSearchResponse> result = assignmentService.searchAssignee(request);
            assertThat(result.getContent()).hasSize(1);
            // Kiểm tra fullName qua đường dẫn getUser().getFullName()
            assertThat(result.getContent().iterator().next().getUser().getFullName()).isEqualTo("Sinh viên A");
        }

        @Test
        @DisplayName("TC_ASS7_002: Bài tập không tồn tại → AppException")
        void searchAssignee_AssignmentNotFound() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(99L)).thenReturn(Optional.empty());

            BaseFilterSearchRequest<AssigneeSearchRequest> request = new BaseFilterSearchRequest<>();
            request.setFilters(new AssigneeSearchRequest());
            request.getFilters().setAssignmentId("99");

            assertThatThrownBy(() -> assignmentService.searchAssignee(request))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS7_003: Object Pagination = null ném AppException")
        void searchAssignee_NullPagination_ShouldThrowBadRequest() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);

            BaseFilterSearchRequest<AssigneeSearchRequest> request = new BaseFilterSearchRequest<>();
            request.setFilters(new AssigneeSearchRequest());
            request.getFilters().setAssignmentId("50");
            request.setPagination(null); // Gây ra NPE khi gọi getPagination().getPagingMeta()

            assertThatThrownBy(() -> assignmentService.searchAssignee(request))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS7_004: Danh sách assignee rỗng → trả về collection trống")
        void searchAssignee_EmptyResult() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockTeacherPermission(100L, 1L);

            when(submissionRepository.searchAssignee(any(AssigneeSearchRequestDTO.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            BaseFilterSearchRequest<AssigneeSearchRequest> request = new BaseFilterSearchRequest<>();
            request.setFilters(new AssigneeSearchRequest());
            request.getFilters().setAssignmentId("50");
            request.setPagination(new SearchRequest());

            ResponseListData<AssigneeSearchResponse> result = assignmentService.searchAssignee(request);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS7_005: Sinh viên truy cập vào classroom hợp lệ → dịch vụ không ném lỗi xác thực")
        void searchAssignee_StudentCanAccess_NoBizRuleCheck() {
            // Ghi chú: code hiện tại chỉ kiểm tra classroom access, không kiểm tra role
            // Đây là điểm thiếu validation → Cần bắn FORBIDDEN
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(sampleAssignment));
            mockStudentAccess(100L, 2L);

            when(submissionRepository.searchAssignee(any(), any()))
                    .thenReturn(Page.empty());

            BaseFilterSearchRequest<AssigneeSearchRequest> request = new BaseFilterSearchRequest<>();
            request.setFilters(new AssigneeSearchRequest());
            request.getFilters().setAssignmentId("50");
            request.setPagination(new SearchRequest());

            // Hiện tại code không throw FORBIDDEN với sinh viên → kỳ vọng ném AppException
            assertThatThrownBy(() -> assignmentService.searchAssignee(request))
                    .isInstanceOf(AppException.class);
        }
    }

    // =================================================================
    //  NHÓM 8: THỐNG KÊ BÀI TẬP
    // =================================================================
    @Nested
    @DisplayName("Nhóm 8: Thống kê bài tập (getAssignmentStatistics)")
    class GetAssignmentStatisticsTests {

        @Test
        @DisplayName("TC_ASS8_001: Giảng viên xem thống kê đầy đủ")
        void getStatistics_Success() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.canUserViewSubmissions(50L, 1L)).thenReturn(true);

            AssignmentOverviewQueryDTO overview = new AssignmentOverviewQueryDTO(
                    30L, 25L, 5L, 2L, 18L, 7.5, 7.0, 10.0);
            when(submissionRepository.getAssignmentOverview(50L)).thenReturn(overview);
            when(submissionRepository.findAllGradesByAssignmentId(50L))
                    .thenReturn(List.of(5.0, 6.0, 7.0, 8.0, 9.0, 10.0));

            AssignmentStatisticResponse response = assignmentService.getAssignmentStatistics("50");
            assertThat(response.getOverview()).isNotNull();
            assertThat(response.getOverview().getAvgGrade()).isEqualTo(7.5);
            assertThat(response.getScoreDistribution()).isNotEmpty();
        }

        @Test
        @DisplayName("TC_ASS8_002: Sinh viên không có quyền xem → FORBIDDEN")
        void getStatistics_Forbidden() {
            when(authService.getCurrentUser()).thenReturn(studentUser);
            when(assignmentRepository.canUserViewSubmissions(50L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> assignmentService.getAssignmentStatistics("50"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("TC_ASS8_003: Chưa có bài nộp → trả về số liệu bằng 0")
        void getStatistics_NoSubmissions() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.canUserViewSubmissions(50L, 1L)).thenReturn(true);
            when(submissionRepository.getAssignmentOverview(50L)).thenReturn(
                    new AssignmentOverviewQueryDTO(20L, 0L, 20L, 0L, 0L, 0.0, 0.0, 0.0));
            when(submissionRepository.findAllGradesByAssignmentId(50L)).thenReturn(Collections.emptyList());

            AssignmentStatisticResponse response = assignmentService.getAssignmentStatistics("50");
            assertThat(response.getOverview().getSubmitted()).isZero();
            assertThat(response.getScoreDistribution()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS8_004: Phân phối điểm với nhiều nhóm điểm khác nhau")
        void getStatistics_ScoreDistribution_AllBuckets() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.canUserViewSubmissions(50L, 1L)).thenReturn(true);
            when(submissionRepository.getAssignmentOverview(50L)).thenReturn(
                    new AssignmentOverviewQueryDTO(10L, 10L, 0L, 0L, 10L, 6.5, 6.0, 8.0));
            // Điểm trải đều qua các khoảng: <5, 5-6.4, 6.5-7.9, 8-10
            List<Double> grades = List.of(4.0, 5.5, 7.0, 8.5, 9.5);
            when(submissionRepository.findAllGradesByAssignmentId(50L)).thenReturn(grades);

            AssignmentStatisticResponse response = assignmentService.getAssignmentStatistics("50");
            // 5 mốc điểm sinh ra 5 khoảng điểm do gradeGroup chia rải đều (4.0, 5.5, 7.0, 8.5, 9.5)
            assertThat(response.getScoreDistribution()).hasSize(5);
        }

        @Test
        @DisplayName("TC_ASS8_005: Tất cả điểm trong cùng một khoảng → chỉ 1 nhóm")
        void getStatistics_ScoreDistribution_SingleBucket() {
            when(authService.getCurrentUser()).thenReturn(teacherUser);
            when(assignmentRepository.canUserViewSubmissions(50L, 1L)).thenReturn(true);
            when(submissionRepository.getAssignmentOverview(50L)).thenReturn(
                    new AssignmentOverviewQueryDTO(5L, 5L, 0L, 0L, 5L, 9.0, 9.0, 10.0));
            // Tất cả đều rơi vào khoảng 9-10
            List<Double> grades = List.of(9.0, 9.1, 9.5, 9.9, 10.0);
            when(submissionRepository.findAllGradesByAssignmentId(50L)).thenReturn(grades);

            AssignmentStatisticResponse response = assignmentService.getAssignmentStatistics("50");
            assertThat(response.getScoreDistribution()).hasSize(1);
        }
    }

    // =================================================================
    //  NHÓM 9: SO SÁNH ĐIỂM TRUNG BÌNH
    // =================================================================
    @Nested
    @DisplayName("Nhóm 9: So sánh điểm trung bình (getAverageScoreComparison)")
    class GetAverageScoreComparisonTests {

        @Test
        @DisplayName("TC_ASS9_001: Trả về điểm trung bình cho tất cả bài tập")
        void averageScoreComparison_Success() {
            when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));
            Assignment a1 = Assignment.builder().assignmentId(1L).title("Bài 1").classroomId(100L).build();
            Assignment a2 = Assignment.builder().assignmentId(2L).title("Bài 2").classroomId(100L).build();
            when(assignmentRepository.findAssignmentsWithGradedSubmissionsByClassroomId(100L))
                    .thenReturn(List.of(a1, a2));
            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(7.5);
            when(submissionRepository.getAverageGradeByAssignmentId(2L)).thenReturn(8.2);

            AssignmentAverageScoreComparisonResponse response = assignmentService.getAverageScoreComparison(100L);
            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData().get(0).getValue()).isEqualTo(7.5);
        }

        @Test
        @DisplayName("TC_ASS9_002: Lớp học không tồn tại → AppException")
        void averageScoreComparison_ClassroomNotFound() {
            when(classroomRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> assignmentService.getAverageScoreComparison(99L))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS9_003: Không có bài tập được chấm → danh sách rỗng")
        void averageScoreComparison_Empty() {
            when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));
            when(assignmentRepository.findAssignmentsWithGradedSubmissionsByClassroomId(100L))
                    .thenReturn(Collections.emptyList());

            AssignmentAverageScoreComparisonResponse response = assignmentService.getAverageScoreComparison(100L);
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS9_004: Bài tập có điểm trung bình null → bỏ qua, không thêm vào kết quả")
        void averageScoreComparison_NullGradeSkipped() {
            when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));
            Assignment a1 = Assignment.builder().assignmentId(1L).title("Bài 1").build();
            when(assignmentRepository.findAssignmentsWithGradedSubmissionsByClassroomId(100L))
                    .thenReturn(List.of(a1));
            // Trả về null → phải bỏ qua
            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(null);

            AssignmentAverageScoreComparisonResponse response = assignmentService.getAverageScoreComparison(100L);
            assertThat(response.getData()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS9_005: Nhiều bài tập, chỉ một số có điểm → chỉ tính những bài có điểm")
        void averageScoreComparison_MixedNullAndNonNull() {
            when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));
            Assignment a1 = Assignment.builder().assignmentId(1L).title("Bài 1").build();
            Assignment a2 = Assignment.builder().assignmentId(2L).title("Bài 2").build();
            Assignment a3 = Assignment.builder().assignmentId(3L).title("Bài 3").build();
            when(assignmentRepository.findAssignmentsWithGradedSubmissionsByClassroomId(100L))
                    .thenReturn(List.of(a1, a2, a3));
            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(7.0);
            when(submissionRepository.getAverageGradeByAssignmentId(2L)).thenReturn(null); // Bỏ qua
            when(submissionRepository.getAverageGradeByAssignmentId(3L)).thenReturn(8.0);

            AssignmentAverageScoreComparisonResponse response = assignmentService.getAverageScoreComparison(100L);
            // Chỉ a1 và a3 có điểm → 2 item
            assertThat(response.getData()).hasSize(2);
        }
    }

    // =================================================================
    //  NHÓM 10: XU HƯỚNG CẢI THIỆN
    // =================================================================
    @Nested
    @DisplayName("Nhóm 10: Xu hướng cải thiện (getImprovementTrend)")
    class GetImprovementTrendTests {

        @Test
        @DisplayName("TC_ASS10_001: Xu hướng theo SESSION, kỳ ALL → 2 điểm dữ liệu, trend IMPROVING")
        void improvementTrend_SessionAll() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));

            LocalDateTime now = LocalDateTime.now();
            // Dùng helper đã gán createdAt để tránh NPE trong service khi gọi getCreatedAt().format(...)
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài 1", now.minusDays(5));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài 2", now.minusDays(1));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(a1, a2));

            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(6.0);
            when(submissionRepository.getAverageGradeByAssignmentId(2L)).thenReturn(7.5);
            when(submissionRepository.findAllGradesByAssignmentId(1L)).thenReturn(List.of(5.0, 7.0));
            when(submissionRepository.findAllGradesByAssignmentId(2L)).thenReturn(List.of(7.0, 8.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(80.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(20.0);
            // Mock findAllByAssignmentId cho calculateTrendStatistics
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertThat(response.getTrendData()).hasSize(2);
            assertThat(response.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.IMPROVING);
        }

        @Test
        @DisplayName("TC_ASS10_002: Lớp học không tồn tại hoặc không active → AppException")
        void improvementTrend_ClassroomNotFound() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(99L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.getImprovementTrend(99L, null, null))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("TC_ASS10_003: Không có bài tập đã qua hạn → trend rỗng, điểm trung bình = 0")
        void improvementTrend_NoCompletedAssignments() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));

            // Bài tập chưa đến hạn → bị lọc ra
            Assignment future = buildAssignmentWithTimestamp(1L, 100L, "Bài tương lai",
                    LocalDateTime.now().plusDays(5));
            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(future));

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertThat(response.getTrendData()).isEmpty();
            assertThat(response.getOverallTrend().getFirstAverageScore()).isZero();
        }

        @Test
        @DisplayName("TC_ASS10_004: Nhóm theo MONTH → nhóm được bài tập vào từng tháng")
        void improvementTrend_GroupByMonth() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài tháng 1", now.minusDays(10));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài tháng 2", now.minusDays(5));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L)).thenReturn(List.of(a1, a2));
            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(8.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(7.0, 9.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(50.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.MONTH);

            assertThat(response.getGroupBy()).isEqualTo(StatisticsGroupBy.MONTH);
            assertThat(response.getTrendData()).isNotEmpty();
        }

        @Test
        @DisplayName("TC_ASS10_005: Lọc theo kỳ MONTH → chỉ lấy bài trong 30 ngày gần nhất")
        void improvementTrend_PeriodMonthFilter() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            // Bài gần → trong vòng 30 ngày (minusDays(10))
            Assignment recent = buildAssignmentWithTimestamp(1L, 100L, "Bài gần", now.minusDays(10));
            // Bài cũ → hơn 30 ngày (minusDays(40)) → bị lọc ra
            Assignment old    = buildAssignmentWithTimestamp(2L, 100L, "Bài cũ",  now.minusDays(40));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(recent, old));
            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(7.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(6.0, 8.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(0.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.MONTH, StatisticsGroupBy.SESSION);

            // Chỉ bài gần được đưa vào
            assertThat(response.getTrendData()).hasSize(1);
            assertThat(response.getTrendData().get(0).getAssignmentTitle()).isEqualTo("Bài gần");
        }

        @Test
        @DisplayName("TC_ASS10_006: period = null và groupBy = null → dùng mặc định ALL + SESSION")
        void improvementTrend_NullParams_UsesDefaults() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            // Không có bài nào → trả về kết quả rỗng không lỗi
            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, null, null);

            // Kết quả mặc định
            assertThat(response.getPeriod()).isEqualTo(StatisticsPeriod.ALL);
            assertThat(response.getGroupBy()).isEqualTo(StatisticsGroupBy.SESSION);
            assertThat(response.getTrendData()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS10_007: Nhóm theo WEEK → các bài trong cùng tuần được gộp lại")
        void improvementTrend_GroupByWeek() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            // 2 bài trong cùng tuần (minusDays(2) và minusDays(3)) → có thể gộp vào 1 nhóm tuần
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài tuần A1", now.minusDays(2));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài tuần A2", now.minusDays(3));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L)).thenReturn(List.of(a1, a2));
            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(7.5);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(7.0, 8.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(50.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.WEEK);

            assertThat(response.getGroupBy()).isEqualTo(StatisticsGroupBy.WEEK);
            assertThat(response.getTrendData()).isNotEmpty();
        }

        @Test
        @DisplayName("TC_ASS10_008: Lọc theo kỳ QUARTER (3 tháng) → chỉ lấy bài trong 90 ngày")
        void improvementTrend_PeriodQuarter() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment inRange  = buildAssignmentWithTimestamp(1L, 100L, "Trong quý", now.minusDays(60));
            Assignment outRange = buildAssignmentWithTimestamp(2L, 100L, "Ngoài quý", now.minusDays(100));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(inRange, outRange));
            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(7.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(7.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(1L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(1L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(0.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.QUARTER, StatisticsGroupBy.SESSION);

            // Chỉ bài trong 90 ngày được tính
            assertThat(response.getTrendData()).hasSize(1);
        }

        @Test
        @DisplayName("TC_ASS10_009: Điểm trung bình trả về null → không tạo TrendDataItem (bị filter null)")
        void improvementTrend_NullAverageGrade_Filtered() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài không điểm", now.minusDays(5));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L)).thenReturn(List.of(a1));
            // Trả về null → service sẽ return null và bị filter
            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(null);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            // null TrendDataItem bị lọc đi → danh sách rỗng
            assertThat(response.getTrendData()).isEmpty();
        }

        @Test
        @DisplayName("TC_ASS10_010: Xu hướng DECLINING khi điểm giảm dần")
        void improvementTrend_Declining() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài 1 điểm cao", now.minusDays(10));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài 2 điểm thấp", now.minusDays(2));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(a1, a2));
            // Điểm giảm từ 9.0 xuống 5.0
            when(submissionRepository.getAverageGradeByAssignmentId(1L)).thenReturn(9.0);
            when(submissionRepository.getAverageGradeByAssignmentId(2L)).thenReturn(5.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(5.0, 9.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(50.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(50.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertThat(response.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.DECLINING);
        }

        @Test
        @DisplayName("TC_ASS10_011: Điểm không đổi → xu hướng STABLE")
        void improvementTrend_Stable() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài ổn định 1", now.minusDays(10));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài ổn định 2", now.minusDays(2));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L))
                    .thenReturn(List.of(a1, a2));
            // Điểm không đổi
            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(7.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(7.0, 7.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(0.0);
            when(submissionRepository.findAllByAssignmentId(anyLong())).thenReturn(Collections.emptyList());

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertThat(response.getOverallTrend().getTrend()).isEqualTo(ImprovementTrend.STABLE);
        }

        @Test
        @DisplayName("TC_ASS10_012: Tính toán chính xác consistentImprovers và decliningStudents")
        void improvementTrend_TrendStatisticsCounters() {
            when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                    .thenReturn(Optional.of(classroom));
            LocalDateTime now = LocalDateTime.now();
            Assignment a1 = buildAssignmentWithTimestamp(1L, 100L, "Bài 1", now.minusDays(10));
            Assignment a2 = buildAssignmentWithTimestamp(2L, 100L, "Bài 2", now.minusDays(2));

            when(assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(100L)).thenReturn(List.of(a1, a2));

            // Student 1: Tăng điểm (Improver) 6.0 -> 8.0
            Submission s1_a1 = Submission.builder().studentId(1L).grade(6.0).submissionStatus(SubmissionStatus.SUBMITTED).build();
            Submission s1_a2 = Submission.builder().studentId(1L).grade(8.0).submissionStatus(SubmissionStatus.LATE_SUBMITTED).build();
            
            // Student 2: Giảm điểm (Declining) 9.0 -> 5.0
            Submission s2_a1 = Submission.builder().studentId(2L).grade(9.0).submissionStatus(SubmissionStatus.SUBMITTED).build();
            Submission s2_a2 = Submission.builder().studentId(2L).grade(5.0).submissionStatus(SubmissionStatus.SUBMITTED).build();

            // Student 3: Bỏ thi bài 2
            Submission s3_a1 = Submission.builder().studentId(3L).grade(7.0).submissionStatus(SubmissionStatus.SUBMITTED).build();
            Submission s3_a2 = Submission.builder().studentId(3L).grade(null).submissionStatus(SubmissionStatus.NOT_SUBMITTED).build();

            when(submissionRepository.findAllByAssignmentId(1L)).thenReturn(List.of(s1_a1, s2_a1, s3_a1));
            when(submissionRepository.findAllByAssignmentId(2L)).thenReturn(List.of(s1_a2, s2_a2, s3_a2));

            when(submissionRepository.getAverageGradeByAssignmentId(anyLong())).thenReturn(7.0);
            when(submissionRepository.findAllGradesByAssignmentId(anyLong())).thenReturn(List.of(7.0));
            when(submissionRepository.countTotalStudentsByAssignmentId(anyLong())).thenReturn(3L);
            when(submissionRepository.countSubmittedStudentsByAssignmentId(anyLong())).thenReturn(2L);
            when(submissionRepository.getPassRateByAssignmentId(anyLong())).thenReturn(100.0);
            when(submissionRepository.getExcellentRateByAssignmentId(anyLong())).thenReturn(50.0);

            AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                    100L, StatisticsPeriod.ALL, StatisticsGroupBy.SESSION);

            assertThat(response.getStatistics().getConsistentImprovers()).isEqualTo(1); // Student 1
            assertThat(response.getStatistics().getDecliningStudents()).isEqualTo(1); // Student 2
        }
    }
}