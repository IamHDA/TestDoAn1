package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.assignment.AssigneeAddRequest;
import com.vn.backend.dto.request.assignment.AssignmentCreateRequest;
import com.vn.backend.dto.request.assignment.AssignmentListRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.assignment.AssignmentListResponse;
import com.vn.backend.dto.response.assignment.AssignmentResponse;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.User;
import com.vn.backend.enums.AnnouncementType;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnnouncementRepository;
import com.vn.backend.repositories.AssignmentRepository;
import com.vn.backend.repositories.AttachmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.SubmissionRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AnnouncementService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.EmailService;
import com.vn.backend.services.SubmissionService;
import com.vn.backend.services.impl.AssignmentServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentServiceImpl Unit Tests")
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private MessageUtils messageUtils;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private User teacherUser;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .fullName("Teacher")
                .role(Role.TEACHER)
                .build();

        classroom = Classroom.builder()
                .classroomId(100L)
                .className("Math 101")
                .teacherId(1L)
                .classroomStatus(ClassroomStatus.ACTIVE)
                .build();
    }

    // ===================== createAssignment =====================

    @Test
    @DisplayName("createAssignment - thành công tạo assignment và thông báo")
    void createAssignment_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true); 

        AssignmentCreateRequest request = new AssignmentCreateRequest();
        request.setTitle("HW 1");
        request.setContent("Do exercise 1 to 5");
        request.setMaxScore(10L);
        request.setSubmissionClosed(false);
        request.setDueDate(LocalDateTime.now().plusDays(2));

        when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment arg = invocation.getArgument(0);
            arg.setAssignmentId(50L);
            return arg;
        });

        Long assignmentId = assignmentService.createAssignment(100L, request);

        assertThat(assignmentId).isEqualTo(50L);
        verify(announcementRepository).save(any(Announcement.class));
        verify(announcementService).notifyAnnouncement(any(Announcement.class));
        verify(submissionService).createDefaultSubmissions(any(Assignment.class));
    }

    // ===================== getAssignmentDetail =====================

    @Test
    @DisplayName("getAssignmentDetail - thành công lấy thông tin assignment")
    void getAssignmentDetail_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        // UserRole không cần kiểm tra vì là Teacher (nó trả ra null trong ClassMemberRepo và method getUserRoleInClassroom trả null nhưng vì là teacher nên bỏ qua if(existsBy...))
        
        Assignment assignment = Assignment.builder()
                .assignmentId(50L)
                .classroomId(100L)
                .title("HW 1")
                .createdByUser(teacherUser)
                .dueDate(LocalDateTime.now().plusDays(1))
                .submissionClosed(false)
                .build();
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(assignment));

        Announcement announcement = new Announcement();
        announcement.setAnnouncementId(200L);
        announcement.setAllowComments(true);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT)).thenReturn(announcement);

        AssignmentResponse response = assignmentService.getAssignmentDetail(50L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("HW 1");
        assertThat(response.getCanEdit()).isTrue();
        assertThat(response.getCanSubmit()).isFalse(); // Vì Teacher không được nộp bài (chỉ STUDENT)
        assertThat(response.getAnnouncementId()).isEqualTo(200L);
    }

    // ===================== getAssignmentList =====================

    @Test
    @DisplayName("getAssignmentList - trả về danh sách có phân trang")
    void getAssignmentList_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        AssignmentListRequest request = new AssignmentListRequest();
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        request.setPagination(pagination);

        Assignment assignment = Assignment.builder().assignmentId(50L).title("HW 1").build();
        Page<Assignment> page = new PageImpl<>(List.of(assignment));

        when(assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(eq(100L), any(Pageable.class)))
                .thenReturn(page);

        ResponseListData<AssignmentListResponse> response = assignmentService.getAssignmentList(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPaging().getTotalRows()).isEqualTo(1);
    }

    // ===================== addAssignee =====================

    @Test
    @DisplayName("addAssignee - thành công thiết lập student")
    void addAssignee_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        Assignment assignment = Assignment.builder().assignmentId(50L).classroomId(100L).build();
        when(assignmentRepository.findByAssignmentId(50L)).thenReturn(Optional.of(assignment));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                100L, 2L, ClassMemberStatus.ACTIVE, ClassMemberRole.STUDENT
        )).thenReturn(true);

        when(submissionRepository.findByAssignmentIdAndStudentId(50L, 2L)).thenReturn(Optional.empty());

        AssigneeAddRequest req = new AssigneeAddRequest();
        req.setUserId("2");
        assignmentService.addAssignee("50", req);

        verify(submissionRepository).save(any());
    }

    // ===================== softDeleteAssignment =====================

    @Test
    @DisplayName("softDeleteAssignment - đổi cờ deleted của assignment và announcement thành true")
    void softDeleteAssignment_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        Assignment assignment = Assignment.builder().assignmentId(50L).classroomId(100L).isDeleted(false).build();
        when(assignmentRepository.findByAssignmentIdAndNotDeleted(50L)).thenReturn(Optional.of(assignment));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true); // Owner or teacher

        Announcement announcement = new Announcement();
        announcement.setIsDeleted(false);
        when(announcementRepository.findByObjectIdAndType(50L, AnnouncementType.ASSIGNMENT)).thenReturn(announcement);

        assignmentService.softDeleteAssignment(50L);

        assertThat(assignment.isDeleted()).isTrue();
        assertThat(announcement.getIsDeleted()).isTrue();
        verify(assignmentRepository).save(assignment);
        verify(announcementRepository).save(announcement);
    }
}
