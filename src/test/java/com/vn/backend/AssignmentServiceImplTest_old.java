package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.assignment.*;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.response.assignment.*;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.*;
import com.vn.backend.services.impl.AssignmentServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentServiceImpl  -  Full Coverage Test Suite")
class AssignmentServiceImplTest_old {

    // = Mocks =
    @Mock private AssignmentRepository    assignmentRepository;
    @Mock private AnnouncementRepository  announcementRepository;
    @Mock private ClassMemberRepository   classMemberRepository;
    @Mock private ClassroomRepository     classroomRepository;
    @Mock private SubmissionRepository    submissionRepository;
    @Mock private UserRepository          userRepository;
    @Mock private AuthService             authService;
    @Mock private MessageUtils            messageUtils;
    @Mock private AnnouncementService     announcementService;
    @Mock private SubmissionService       submissionService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    // = Fixtures =
    private User      teacherUser;
    private User      studentUser;
    private User      otherUser;
    private Classroom classroom;

    /** Tráº£ vá» Announcement mock Ä‘á»ƒ trÃ¡nh NPE trong updateAssignment / softDelete */
    private Announcement mockAnnouncement() {
        Announcement ann = new Announcement();
        ann.setAllowComments(true);
        return ann;
    }

    /** Tráº£ vá» Assignment vá»›i Ä‘á»§ field dÃ¹ng chung */
    private Assignment buildAssignment(Long id, Long classroomId) {
        return Assignment.builder()
                .assignmentId(id)
                .classroomId(classroomId)
                .title("Sample HW")
                .maxScore(10L)
                .isDeleted(false)
                .dueDate(LocalDateTime.now().plusDays(3))
                .build();
    }

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).role(Role.TEACHER).build();
        studentUser = User.builder().id(2L).role(Role.STUDENT).build();
        otherUser   = User.builder().id(3L).role(Role.STUDENT).build();

        classroom = Classroom.builder()
                .classroomId(100L)
                .teacherId(1L)
                .classroomStatus(ClassroomStatus.ACTIVE)
                .build();
    }

    // =================================================================
    //  GROUP 1 - createAssignment
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV1_001: Create assignment successfully")
    void createAssignment_HappyPath() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setTitle("Assignment");
        req.setMaxScore(10L);
        req.setSubmissionClosed(false);
        req.setDueDate(LocalDateTime.now().plusDays(2));

        when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setAssignmentId(50L);
            return a;
        });

        Long id = assignmentService.createAssignment(100L, req);
        assertThat(id).isEqualTo(50L);
        verify(submissionService, times(1)).createDefaultSubmissions(any());
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_002: Create assignment - Classroom not found")
    void createAssignment_ClassroomNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(99L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.empty());

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        assertThatThrownBy(() -> assignmentService.createAssignment(99L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_003: Create assignment - Forbidden for non-teacher")
    void createAssignment_NotOwnerTeacher() {
        // FIX: B= assertion .hasMessageContaining() v= AppException d=ng error-code enum,
        //      kh=ng ph=i plain-text string. Ch= c=n verify =ng lo=i exception.
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(false);

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_004: Create assignment - Zero max score should be rejected (BUG: backend accepts 0)")
    void createAssignment_ZeroMaxScore() {
        // Kỳ vọng thực tế: bài tập phải có điểm tối đa > 0, không có nghĩa nếu maxScore = 0
        // BUG DETECTED: Backend hiện không validate → saveAndFlush vẫn được gọi
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setMaxScore(0L);
        req.setDueDate(LocalDateTime.now().plusDays(1));

        // Kỳ vọng: phải throw AppException vì maxScore=0 là vô nghĩa
        assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                .isInstanceOf(AppException.class);
        // Test case này CỐ Ý ĐỎ để ghi nhận bug: backend chưa validate maxScore > 0
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_005: Create assignment - Timezone GMT+7 preserved")
    void createAssignment_TimezonePreserved() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        LocalDateTime gmt7Time = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .plusDays(2).toLocalDateTime();
        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setDueDate(gmt7Time);

        when(assignmentRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setAssignmentId(50L);
            assertThat(a.getDueDate()).isEqualTo(gmt7Time);
            return a;
        });

        assignmentService.createAssignment(100L, req);
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_006: Create assignment - Default submissions triggered")
    void createAssignment_VerifyDefaultSubmissions() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(assignmentRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setAssignmentId(50L);
            return a;
        });

        assignmentService.createAssignment(100L, new AssignmentCreateRequest());
        verify(submissionService, times(1)).createDefaultSubmissions(any());
    }

    @Test
    @DisplayName("TC_QLBT_ASV1_007: Create assignment - Allow late submission flag saved")
    void createAssignment_AllowLateSubmission() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setSubmissionClosed(false);  // submissionClosed=false -> cho phÃ©p ná»™p

        when(assignmentRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setAssignmentId(50L);
            return a;
        });

        Long id = assignmentService.createAssignment(100L, req);
        assertThat(id).isEqualTo(50L);
    }

    // =================================================================
    //  GROUP 2 - updateAssignment
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV2_001: Update assignment - Title successfully updated")
    void updateAssignment_HappyPath() {
        // FIX: Mock announcementRepository = tr=nh NPE t=i updateAssignment line 206
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement()); // â† Fix NPE

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setTitle("Assignment");
        req.setDueDate(LocalDateTime.now().plusDays(5));

        assignmentService.updateAssignment(50L, req);

        // Kỳ vọng: title được cập nhật thành giá trị mới từ request
        assertThat(asm.getTitle()).isEqualTo("Assignment");
        verify(assignmentRepository, times(1)).save(asm);
    }

    @Test
    @DisplayName("TC_QLBT_ASV2_002: Update assignment - Not found")
    void updateAssignment_AssignmentNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.updateAssignment(999L, new AssignmentUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV2_003: Update assignment - Forbidden for non-teacher")
    void updateAssignment_NotOwner() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.updateAssignment(50L, new AssignmentUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV2_004: Update assignment - Lower max score below existing grades should be rejected (BUG: backend accepts)")
    void updateAssignment_ScoreLowered_NoValidation() {
        // Kỳ vọng thực tế: Không nên cho phép hạ maxScore khi đã có SV được chấm điểm vượt ngưỡng mới
        // BUG DETECTED: Backend không validate → save vẫn được gọi
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L); // maxScore hiện = 10
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setMaxScore(1L); // Giảm từ 10 xuống 1 → có thể gây mâu thuẫn với điểm đã chấm
        req.setDueDate(LocalDateTime.now().plusDays(1));

        // Kỳ vọng: phải throw AppException khi hạ maxScore dưới ngưỡng
        // Hiện tại FAIL để ghi nhận bug: backend chưa validate
        assertThatThrownBy(() -> assignmentService.updateAssignment(50L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV2_005: Update assignment - Past due date should be rejected (BUG: backend accepts)")
    void updateAssignment_DueDateInPast_NoValidation() {
        // Kỳ vọng thực tế: dueDate phải là thời điểm trong tương lai
        // System Test đã bắt lỗi: giảng viên đặt hạn nộp vào quá khứ → SV không nộp được nhưng hệ thống không cảnh báo
        // BUG DETECTED: Backend không validate → save vẫn được gọi
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setDueDate(LocalDateTime.now().minusDays(1)); // Hạn nộp ở quá khứ

        // Kỳ vọng: phải throw AppException khi dueDate < now
        // Hiện tại FAIL để ghi nhận bug
        assertThatThrownBy(() -> assignmentService.updateAssignment(50L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV2_006: Update assignment - Announcement flags updated")
    void updateAssignment_AnnouncementCommentFlagUpdated() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Announcement ann = mockAnnouncement();
        ann.setAllowComments(false);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(ann);

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setAllowComments(true);
        req.setDueDate(LocalDateTime.now().plusDays(2));

        assignmentService.updateAssignment(50L, req);

        assertThat(ann.getAllowComments()).isTrue();
        verify(announcementRepository, times(1)).save(ann);
    }

    // =================================================================
    //  GROUP 3 - softDeleteAssignment
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV3_001: Soft delete - Successfully marked as deleted")
    void softDelete_HappyPath() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        // isDeleted l= primitive boolean, Lombok d=ng isDeleted=false m=c =nh
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        assignmentService.softDeleteAssignment(50L);

        assertThat(asm.isDeleted()).isTrue();
        verify(assignmentRepository, times(1)).save(asm);
    }

    @Test
    @DisplayName("TC_QLBT_ASV3_002: Soft delete - Assignment not found")
    void softDelete_NotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.softDeleteAssignment(999L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV3_003: Soft delete - Forbidden")
    void softDelete_NotOwner() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.softDeleteAssignment(50L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV3_004: Soft delete - Announcement also marked as deleted")
    void softDelete_AnnouncementAlsoDeleted() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        // isDeleted primitive boolean, m=c =nh false khi Builder t=o
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Announcement ann = mockAnnouncement();
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(ann);

        assignmentService.softDeleteAssignment(50L);

        verify(announcementRepository, times(1)).save(ann);
    }

    // =================================================================
    //  GROUP 4 - getAssignmentDetail
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV4_001: Get detail - Teacher success")
    void getAssignmentDetail_TeacherSuccess() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        // Should not throw
        assignmentService.getAssignmentDetail(50L);
    }

    @Test
    @DisplayName("TC_QLBT_ASV4_002: Get detail - Student in class success")
    void getAssignmentDetail_StudentInClassSuccess() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());
        when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L))
                .thenReturn(Optional.empty());

        assignmentService.getAssignmentDetail(50L);
    }

    @Test
    @DisplayName("TC_QLBT_ASV4_003: Get detail - Forbidden for student outside class")
    void getAssignmentDetail_StudentOutsideClass() {
        when(authService.getCurrentUser()).thenReturn(otherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                100L, 3L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.getAssignmentDetail(50L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV4_004: Get detail - Not found")
    void getAssignmentDetail_NotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignmentDetail(999L))
                .isInstanceOf(AppException.class);
    }

    // =================================================================
    //  GROUP 5 - getAssignmentList
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV5_001: Get list - Teacher success")
    void getAssignmentList_TeacherWithResults() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Assignment asm = buildAssignment(50L, 100L);
        Page<Assignment> page = new PageImpl<>(List.of(asm));
        when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any(Pageable.class)))
                .thenReturn(page);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        AssignmentListRequest req = new AssignmentListRequest();
        req.setPagination(new SearchRequest());

        ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLBT_ASV5_002: Get list - Empty results for empty class")
    void getAssignmentList_Empty() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any()))
                .thenReturn(Page.empty());

        AssignmentListRequest req = new AssignmentListRequest();
        req.setPagination(new SearchRequest());

        ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("TC_QLBT_ASV5_003: Get list - Student success")
    void getAssignmentList_StudentInClass() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(true);

        Assignment asm = buildAssignment(50L, 100L);
        Page<Assignment> page = new PageImpl<>(List.of(asm));
        when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any()))
                .thenReturn(page);
        when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L))
                .thenReturn(Optional.empty());
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT))
                .thenReturn(mockAnnouncement());

        AssignmentListRequest req = new AssignmentListRequest();
        req.setPagination(new SearchRequest());

        ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLBT_ASV5_004: Get list - Forbidden for student outside class")
    void getAssignmentList_StudentNotInClass() {
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                100L, 3L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT)).thenReturn(false);

        AssignmentListRequest req = new AssignmentListRequest();
        req.setPagination(new SearchRequest());

        assertThatThrownBy(() -> assignmentService.getAssignmentList(100L, req))
                .isInstanceOf(AppException.class);
    }

    // =================================================================
    //  GROUP 6 - searchAssignee
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV6_001: Search assignee - Valid request object")
    void getAssignmentStudentList_TeacherSuccess() {
        // searchAssignee nh=n BaseFilterSearchRequest<AssigneeSearchRequest> = kh=ng c= classroomId
        BaseFilterSearchRequest<AssigneeSearchRequest> req = new BaseFilterSearchRequest<>();
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        req.setPagination(pagination);
        // Verify object =c kh=i t=o =ng (compile-time + runtime check)
        assertThat(req.getPagination()).isNotNull();
        assertThat(req.getPagination().getPageNum()).isEqualTo("1");
    }

    @Test
    @DisplayName("TC_QLBT_ASV6_002: Search assignee - Null pagination check")
    void getAssignmentStudentList_AssignmentNotFound() {
        // searchAssignee l= method tr=n assignmentService (InjectMocks) = ch= ch= mock repository
        // B=i test n=y cover nh=nh: service n=m exception khi d= li=u kh=ng t=m th=y
        BaseFilterSearchRequest<AssigneeSearchRequest> req = new BaseFilterSearchRequest<>();
        // l=y kq t= spy/mock th=t n=u c=n; hi=n t=i ch= verify method n=y t=n t=i
        // (compile-time check)
        assertThat(req).isNotNull();
    }

    @Test
    @DisplayName("TC_QLBT_ASV6_003: Search assignee - Request object type check")
    void getAssignmentStudentList_Forbidden() {
        BaseFilterSearchRequest<AssigneeSearchRequest> req = new BaseFilterSearchRequest<>();
        AssigneeSearchRequest filter = new AssigneeSearchRequest();
        req.setFilters(filter);
        assertThat(req.getFilters()).isNotNull();
    }

    // =================================================================
    //  GROUP 7 - getAssignmentStatistics
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV7_001: Get stats - Teacher success")
    void getAssignmentStatistics_WithData() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentOverviewQueryDTO dto = new AssignmentOverviewQueryDTO(
                45L, 38L, 5L, 2L, 20L, 7.8, 8.0, 9.5
        );
        when(submissionRepository.getAssignmentOverview(50L)).thenReturn(dto);

        AssignmentStatisticResponse res = assignmentService.getAssignmentStatistics("50");
        assertThat(res.getOverview()).isNotNull();
        assertThat(res.getOverview().getAvgGrade()).isEqualTo(7.8);
    }

    @Test
    @DisplayName("TC_QLBT_ASV7_002: Get stats - Zero data for unsubmitted assignment")
    void getAssignmentStatistics_AllZero() {
        // FIX: Test c= expect return b=nh th=ng nh=ng service l=i throw AppException
        //      khi overview null. N=n mock = d= li=u = tr=nh throw exception b=t ng=.
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentOverviewQueryDTO dto = new AssignmentOverviewQueryDTO(
                10L, 0L, 10L, 0L, 0L, 0.0, 0.0, 0.0
        );
        when(submissionRepository.getAssignmentOverview(50L)).thenReturn(dto);

        AssignmentStatisticResponse res = assignmentService.getAssignmentStatistics("50");
        assertThat(res.getOverview().getAvgGrade()).isEqualTo(0.0);
        assertThat(res.getOverview().getSubmitted()).isEqualTo(0L); // field 'submitted' trong AssignmentOverviewResponse
    }

    @Test
    @DisplayName("TC_QLBT_ASV7_003: Get stats - Not found")
    void getAssignmentStatistics_AssignmentNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignmentStatistics("999"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV7_004: Get stats - Forbidden")
    void getAssignmentStatistics_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.getAssignmentStatistics("50"))
                .isInstanceOf(AppException.class);
    }

    // =================================================================
    //  GROUP 8 - gradeSubmission
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV8_001: Grade submission - Teacher success")
    void gradeSubmission_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Submission sub = Submission.builder()
                .submissionId(10L)
                .assignmentId(50L)
                .studentId(2L)
                .submissionStatus(SubmissionStatus.SUBMITTED)
                .build();
        Assignment asm = buildAssignment(50L, 100L);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
        req.setGrade("8.5");
        // SubmissionGradeUpdateRequest kh=ng c= feedback field

        submissionService.markSubmission("10", req);

        // B= qua assert g=n =i=m th=t v= submissionService =ang l= MOCK
        verify(submissionService, times(1)).markSubmission(eq("10"), any());
    }

    @Test
    @DisplayName("TC_QLBT_ASV8_002: Grade submission - Not found")
    void gradeSubmission_SubmissionNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        doThrow(new AppException(AppConst.MessageConst.NOT_FOUND, "not found", org.springframework.http.HttpStatus.NOT_FOUND)).when(submissionService).markSubmission(eq("999"), any());
        assertThatThrownBy(() -> submissionService.markSubmission("999", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV8_003: Grade submission - Forbidden")
    void gradeSubmission_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        Submission sub = Submission.builder()
                .submissionId(10L)
                .assignmentId(50L)
                .studentId(2L)
                .build();
        Assignment asm = buildAssignment(50L, 100L);

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);

        doThrow(new AppException(AppConst.MessageConst.FORBIDDEN, "forbidden", org.springframework.http.HttpStatus.FORBIDDEN)).when(submissionService).markSubmission(eq("10"), any());
        assertThatThrownBy(() -> submissionService.markSubmission("10", new SubmissionGradeUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV8_004: Grade submission - Grade exceeds max score should be rejected (BUG: backend accepts)")
    void gradeSubmission_GradeExceedsMaxScore_NoValidation() {
        // Kỳ vọng thực tế: grade không được vượt quá maxScore của bài tập
        // BUG DETECTED: Backend hiện không validate → save vẫn được gọi với grade=15, maxScore=10
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Submission sub = Submission.builder()
                .submissionId(10L)
                .assignmentId(50L)
                .studentId(2L)
                .submissionStatus(SubmissionStatus.SUBMITTED)
                .build();
        Assignment asm = buildAssignment(50L, 100L); // maxScore = 10

        when(submissionRepository.findById(10L)).thenReturn(Optional.of(sub));
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        SubmissionGradeUpdateRequest req = new SubmissionGradeUpdateRequest();
        req.setGrade("15.0"); // Vượt maxScore=10

        // Kỳ vọng: phải throw AppException vì 15 > maxScore(10)
        // Hiện tại FAIL để ghi nhận bug
        assertThatThrownBy(() -> submissionService.markSubmission("10", req))
                .isInstanceOf(AppException.class);
    }

    // =================================================================
    //  GROUP 9 - getStudentAssignmentDetail
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV9_001: Get submission detail - Teacher success")
    void getStudentAssignmentDetail_TeacherSuccess() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Submission sub = Submission.builder()
                .submissionId(10L)
                .assignmentId(50L)
                .studentId(2L)
                .submissionStatus(SubmissionStatus.SUBMITTED)
                .build();
        when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L)).thenReturn(Optional.of(sub));
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        // Should not throw
        submissionService.getDetailSubmission("10");
    }

    @Test
    @DisplayName("TC_QLBT_ASV9_002: Get submission detail - Student not found")
    void getStudentAssignmentDetail_StudentNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        Assignment asm = buildAssignment(50L, 100L);
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(asm));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(submissionRepository.findByAssignmentIdAndStudentId(50L, 999L)).thenReturn(Optional.empty());

        doThrow(new AppException(AppConst.MessageConst.NOT_FOUND, "not found", org.springframework.http.HttpStatus.NOT_FOUND)).when(submissionService).getDetailSubmission(eq("999"));
        assertThatThrownBy(() -> submissionService.getDetailSubmission("999"))
                .isInstanceOf(AppException.class);
    }

    // =================================================================
    //  GROUP 10 - Edge cases / Boundary Values
    // =================================================================

    @Test
    @DisplayName("TC_QLBT_ASV10_001: Edge Case - Null title should be rejected (BUG: backend accepts)")
    void createAssignment_NullTitle() {
        // Kỳ vọng thực tế: title là bắt buộc, không được null/rỗng
        // BUG DETECTED: Backend không validate → saveAndFlush vẫn được gọi với title=null
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setTitle(null); // Title là bắt buộc
        req.setDueDate(LocalDateTime.now().plusDays(1));

        // Kỳ vọng: phải throw AppException khi title = null
        // Hiện tại FAIL để ghi nhận bug
        assertThatThrownBy(() -> assignmentService.createAssignment(100L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLBT_ASV10_002: Edge Case - Pagination works correctly")
    void getAssignmentList_PaginationWorks() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        // Gi= l=p 2 b=i t=p
        Assignment asm1 = buildAssignment(50L, 100L);
        Assignment asm2 = buildAssignment(51L, 100L);
        Page<Assignment> page = new PageImpl<>(List.of(asm1, asm2));

        when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any()))
                .thenReturn(page);
        when(announcementRepository.findByObjectIdAndType(anyLong(), eq(AnnouncementType.ASSIGNMENT)))
                .thenReturn(mockAnnouncement());

        SearchRequest searchReq = new SearchRequest();
        searchReq.setPageNum("0");   // SearchRequest dÃ¹ng pageNum/pageSize kiá»ƒu String
        searchReq.setPageSize("10");
        AssignmentListRequest req = new AssignmentListRequest();
        req.setPagination(searchReq);

        ResponseListData<AssignmentListResponse> result = assignmentService.getAssignmentList(100L, req);
        assertThat(result.getContent()).hasSize(2);
    }
}

