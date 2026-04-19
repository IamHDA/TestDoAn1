package com.vn.backend;

import com.vn.backend.dto.request.comment.*;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.entities.*;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnnouncementRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.CommentRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.CommentServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommentServiceImpl Unit Tests")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private AuthService authService;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User teacherUser;
    private User studentUser;
    private User assistantUser;
    private Classroom classroom;
    private Announcement announcement;
    private Comment commentByStudent;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).fullName("Giao Vien").email("gv@test.com").avatarUrl("ava1").build();
        studentUser = User.builder().id(2L).fullName("Sinh Vien").email("sv@test.com").avatarUrl("ava2").build();
        assistantUser = User.builder().id(3L).fullName("Trợ Giảng").email("tg@test.com").avatarUrl("ava3").build();
        
        classroom = Classroom.builder().classroomId(100L).teacherId(1L).build();
        announcement = Announcement.builder().announcementId(500L).classroomId(100L).allowComments(true).build();
        
        commentByStudent = Comment.builder()
                .commentId(10L).announcementId(500L).userId(2L).user(studentUser)
                .content("Bình luận của Sv").isDeleted(false).build();

        when(messageUtils.getMessage(anyString())).thenReturn("Error message");
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findById(anyLong())).thenReturn(Optional.of(announcement));
        
        // Mock mặc định user 2 là ACTIVE member
        ClassMember studentMember = ClassMember.builder().userId(2L).memberStatus(ClassMemberStatus.ACTIVE).memberRole(ClassMemberRole.STUDENT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));
    }

    // ================== createComment ==================
    @Test
    @DisplayName("TC_QLLH_CMT_01: createComment - ném lỗi khi Announcement cấm bình luận")
    void createComment_ForbiddenBySetting() {
        announcement.setAllowComments(false);
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Test");

        assertThatThrownBy(() -> commentService.createComment(500L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_CMT_02: createComment - thành công và trả về đúng thông tin user")
    void createComment_Success() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Bình luận mới");

        when(commentRepository.saveAndFlush(any(Comment.class))).thenReturn(commentByStudent);

        var result = commentService.createComment(500L, request);

        assertThat(result.getContent()).isEqualTo("Bình luận của Sv");
        assertThat(result.getUserFullName()).isEqualTo("Sinh Vien");
        verify(commentRepository).saveAndFlush(any(Comment.class));
    }

    // ================== updateComment ==================
    @Test
    @DisplayName("TC_QLLH_CMT_03: updateComment - ném FORBIDDEN khi sửa bình luận của người khác")
    void updateComment_ForbiddenOwnership() {
        // User hiện tại là student (ID=2)
        Comment commentOfTeacher = Comment.builder().commentId(20L).announcementId(500L).userId(1L).user(teacherUser).build();
        when(commentRepository.findByCommentIdAndNotDeleted(20L)).thenReturn(Optional.of(commentOfTeacher));
        
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setCommentId(20L);
        request.setContent("Sửa trộm");

        assertThatThrownBy(() -> commentService.updateComment(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_CMT_04: updateComment - CHÍNH CHỦ sửa thành công")
    void updateComment_SuccessOwner() {
        when(commentRepository.findByCommentIdAndNotDeleted(10L)).thenReturn(Optional.of(commentByStudent));
        
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setCommentId(10L);
        request.setContent("Đã sửa");

        commentService.updateComment(request);

        assertThat(commentByStudent.getContent()).isEqualTo("Đã sửa");
        verify(commentRepository).saveAndFlush(commentByStudent);
    }

    // ================== deleteComment (Phân quyền quan trọng) ==================
    @Test
    @DisplayName("TC_QLLH_CMT_05: deleteComment - GIÁO VIÊN xóa bình luận của SV thành công")
    void deleteComment_TeacherDeletesStudent() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID=1
        when(commentRepository.findByCommentIdAndNotDeleted(10L)).thenReturn(Optional.of(commentByStudent));
        // Check ID=1 có phải teacher của class 100 ko?
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(10L);

        commentService.deleteComment(request);

        verify(commentRepository).softDeleteById(10L);
    }

    @Test
    @DisplayName("TC_QLLH_CMT_06: deleteComment - TRỢ GIẢNG xóa bình luận SV thành công")
    void deleteComment_AssistantDeletesStudent() {
        when(authService.getCurrentUser()).thenReturn(assistantUser); // ID=3
        when(commentRepository.findByCommentIdAndNotDeleted(10L)).thenReturn(Optional.of(commentByStudent));
        
        // ID=3 là assistant của class 100
        ClassMember assistantMember = ClassMember.builder().userId(3L).memberRole(ClassMemberRole.ASSISTANT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.of(assistantMember));
        
        // Bình luận của SV (ID=2)
        ClassMember studentMember = ClassMember.builder().userId(2L).memberRole(ClassMemberRole.STUDENT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));

        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(10L);

        commentService.deleteComment(request);

        verify(commentRepository).softDeleteById(10L);
    }

    @Test
    @DisplayName("TC_QLLH_CMT_07: deleteComment - TRỢ GIẢNG xóa bình luận GIÁO VIÊN ném FORBIDDEN")
    void deleteComment_AssistantDeletesTeacher_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(assistantUser); // ID=3
        
        Comment commentOfTeacher = Comment.builder().commentId(20L).announcementId(500L).userId(1L).user(teacherUser).build();
        when(commentRepository.findByCommentIdAndNotDeleted(20L)).thenReturn(Optional.of(commentOfTeacher));
        
        ClassMember assistantMember = ClassMember.builder().userId(3L).memberRole(ClassMemberRole.ASSISTANT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.of(assistantMember));

        // Chủ nhân bình luận (ID=1) không phải student trong lớp này (Id=1 là teacher)
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(20L);

        assertThatThrownBy(() -> commentService.deleteComment(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_CMT_08: deleteComment - SINH VIÊN xóa bình luận SV khác ném FORBIDDEN")
    void deleteComment_StudentDeletesOtherStudent_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // ID=2
        
        Comment otherStudentComment = Comment.builder().commentId(30L).announcementId(500L).userId(99L).user(new User()).build();
        when(commentRepository.findByCommentIdAndNotDeleted(30L)).thenReturn(Optional.of(otherStudentComment));
        
        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(30L);

        assertThatThrownBy(() -> commentService.deleteComment(request))
                .isInstanceOf(AppException.class);
    }

    // ================== getCommentList ==================
    @Test
    @DisplayName("TC_QLLH_CMT_09: getCommentList - thành công kèm phân trang")
    void getList_Success() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        
        CommentListRequest request = new CommentListRequest();
        request.setAnnouncementId(500L);
        request.setPagination(new SearchRequest());

        Page<Comment> page = new PageImpl<>(List.of(commentByStudent));
        when(commentRepository.findByAnnouncementIdAndNotDeleted(eq(500L), any(Pageable.class))).thenReturn(page);

        var result = commentService.getCommentList(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC_QLLH_CMT_10: getCommentList - ném lỗi khi user không có quyền vào lớp của announcement")
    void getList_NoAccess() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        // User là member của lớp 100 nhưng Announcement này lại thuộc về lớp 999 (giả sử)
        announcement.setClassroomId(999L);
        when(classMemberRepository.findByClassroomIdAndUserId(999L, 2L)).thenReturn(Optional.empty());

        CommentListRequest request = new CommentListRequest();
        request.setAnnouncementId(500L);

        assertThatThrownBy(() -> commentService.getCommentList(request))
                .isInstanceOf(AppException.class);
    }
}
