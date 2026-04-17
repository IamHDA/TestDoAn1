package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.comment.CommentCreateRequest;
import com.vn.backend.dto.request.comment.CommentDeleteRequest;
import com.vn.backend.dto.request.comment.CommentUpdateRequest;
import com.vn.backend.dto.response.comment.CommentResponse;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Comment;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.Role;
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
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl Unit Tests")
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AuthService authService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User studentUser;
    private User teacherUser;
    private Announcement announcement;
    private Comment existingComment;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(1L)
                .username("student1")
                .role(Role.STUDENT)
                .fullName("Student Name")
                .build();

        teacherUser = User.builder()
                .id(2L)
                .username("teacher1")
                .role(Role.TEACHER)
                .fullName("Teacher Name")
                .build();

        announcement = Announcement.builder()
                .announcementId(10L)
                .classroomId(100L)
                .allowComments(true)
                .build();

        existingComment = Comment.builder()
                .commentId(50L)
                .announcementId(10L)
                .userId(1L)
                .user(studentUser)
                .content("Old Content")
                .isDeleted(false)
                .build();
    }

    // ===================== createComment =====================

    @Test
    @DisplayName("createComment - thành công khi user là member hợp lệ")
    void createComment_Success() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("This is a comment");

        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(false);

        ClassMember classMember = ClassMember.builder()
                .classroomId(100L)
                .userId(1L)
                .memberStatus(ClassMemberStatus.ACTIVE)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(classMember));

        Comment savedComment = Comment.builder()
                .commentId(51L)
                .announcementId(10L)
                .userId(1L)
                .user(studentUser)
                .content("This is a comment")
                .build();
        
        when(commentRepository.saveAndFlush(any(Comment.class))).thenReturn(savedComment);
        
        CommentResponse response = commentService.createComment(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("This is a comment");
        verify(commentRepository).saveAndFlush(any(Comment.class));
    }

    @Test
    @DisplayName("createComment - ném exception khi announcement không cho phép comment")
    void createComment_ThrowsException_WhenCommentNotAllowed() {
        announcement.setAllowComments(false);
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("This is a comment");

        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(false);

        ClassMember classMember = ClassMember.builder()
                .classroomId(100L)
                .userId(1L)
                .memberStatus(ClassMemberStatus.ACTIVE)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(classMember));
        when(messageUtils.getMessage(AppConst.MessageConst.COMMENT_NOT_ALLOW)).thenReturn("Comment not allowed");

        assertThatThrownBy(() -> commentService.createComment(10L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FORBIDDEN);
                });
    }

    // ===================== updateComment =====================

    @Test
    @DisplayName("updateComment - thành công khi user là tác giả comment")
    void updateComment_Success_WhenIsOwner() {
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setCommentId(50L);
        request.setContent("Updated Content");

        when(authService.getCurrentUser()).thenReturn(studentUser); // userId = 1, same as comment.userId
        when(commentRepository.findByCommentIdAndNotDeleted(50L)).thenReturn(Optional.of(existingComment));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse response = commentService.updateComment(request);

        assertThat(response.getContent()).isEqualTo("Updated Content");
        assertThat(existingComment.getContent()).isEqualTo("Updated Content");
        verify(commentRepository).saveAndFlush(existingComment);
    }

    @Test
    @DisplayName("updateComment - ném exception khi user không phải tác giả (permission denied)")
    void updateComment_ThrowsException_WhenPermissionDenied() {
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setCommentId(50L);
        request.setContent("Updated Content");

        // User ID = 99 (Not the owner)
        User otherUser = User.builder().id(99L).username("other").build();

        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(commentRepository.findByCommentIdAndNotDeleted(50L)).thenReturn(Optional.of(existingComment));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 99L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 99L)).thenReturn(Optional.empty()); // Not even in class, or just a student
        
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");
        
        // This will actually throw NOT_FOUND from line 219 inside validatePermission if classMember is empty
        assertThatThrownBy(() -> commentService.updateComment(request))
                .isInstanceOf(AppException.class);
    }

    // ===================== deleteComment =====================

    @Test
    @DisplayName("deleteComment - thành công khi user là teacher của lớp")
    void deleteComment_Success_WhenTeacher() {
        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(50L);

        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID = 2
        when(commentRepository.findByCommentIdAndNotDeleted(50L)).thenReturn(Optional.of(existingComment));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(true); 

        // Teacher có quyền DELETE mọi comment trong lớp
        commentService.deleteComment(request);

        verify(commentRepository).softDeleteById(50L);
    }

    @Test
    @DisplayName("deleteComment - thành công khi user là assistant và xóa comment của student")
    void deleteComment_Success_WhenAssistant() {
        User assistantUser = User.builder().id(3L).build();
        
        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(50L);

        when(authService.getCurrentUser()).thenReturn(assistantUser);
        when(commentRepository.findByCommentIdAndNotDeleted(50L)).thenReturn(Optional.of(existingComment));
        when(announcementRepository.findById(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 3L)).thenReturn(false);

        ClassMember assistantMember = ClassMember.builder()
                .memberRole(ClassMemberRole.ASSISTANT)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 3L)).thenReturn(Optional.of(assistantMember));

        ClassMember ownerMember = ClassMember.builder()
                .memberRole(ClassMemberRole.STUDENT)
                .build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 1L)).thenReturn(Optional.of(ownerMember)); // comment owner is student

        commentService.deleteComment(request);

        verify(commentRepository).softDeleteById(50L);
    }
}
