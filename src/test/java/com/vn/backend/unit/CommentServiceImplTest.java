package com.vn.backend.unit;


import org.junit.jupiter.api.DisplayName;

import com.vn.backend.dto.request.comment.CommentCreateRequest;
import com.vn.backend.dto.request.comment.CommentDeleteRequest;
import com.vn.backend.dto.request.comment.CommentListRequest;
import com.vn.backend.dto.request.comment.CommentUpdateRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.comment.CommentResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Comment;
import com.vn.backend.entities.User;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho CommentServiceImpl, quản lý các unit test cho chức năng bình
 * luận.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {

    private static final Long USER_ID = 4L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long COMMENT_OWNER_ID = 4L;
    private static final Long ANNOUNCEMENT_ID = 10L;
    private static final Long CLASSROOM_ID = 20L;

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

    private CommentServiceImpl service;

    private final Map<Long, Comment> commentStore = new HashMap<>();
    private final AtomicLong commentIds = new AtomicLong(1);

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử.
     */
    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new CommentServiceImpl(
                commentRepository,
                announcementRepository,
                classMemberRepository,
                classroomRepository,
                authService,
                messageUtils);

        mockCommentRepositoryStorage();
    }

    private void mockCommentRepositoryStorage() {
        when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            if (comment.getCommentId() == null) {
                comment.setCommentId(commentIds.getAndIncrement());
            }
            commentStore.put(comment.getCommentId(), comment);
            return comment;
        });

        when(commentRepository.findByCommentIdAndNotDeleted(anyLong()))
                .thenAnswer(invocation -> {
                    Long commentId = invocation.getArgument(0);
                    Comment comment = commentStore.get(commentId);

                    if (comment == null || Boolean.TRUE.equals(comment.getIsDeleted())) {
                        return Optional.empty();
                    }

                    return Optional.of(comment);
                });

        doAnswer(invocation -> {
            Long commentId = invocation.getArgument(0);
            Comment comment = commentStore.get(commentId);
            if (comment != null) {
                comment.setIsDeleted(true);
            }
            return null;
        }).when(commentRepository).softDeleteById(anyLong());
    }

    private User user(Long userId) {
        return User.builder()
                .id(userId)
                .fullName("User " + userId)
                .email("user" + userId + "@example.com")
                .avatarUrl("avatar-" + userId + ".png")
                .build();
    }

    private void mockCurrentUser(Long userId) {
        when(authService.getCurrentUser()).thenReturn(user(userId));
    }

    private Announcement announcement(boolean allowComments) {
        return Announcement.builder()
                .announcementId(ANNOUNCEMENT_ID)
                .classroomId(CLASSROOM_ID)
                .allowComments(allowComments)
                .build();
    }

    private void mockAnnouncement(boolean allowComments) {
        when(announcementRepository.findById(ANNOUNCEMENT_ID))
                .thenReturn(Optional.of(announcement(allowComments)));
    }

    private void mockAnnouncementMissing() {
        when(announcementRepository.findById(ANNOUNCEMENT_ID))
                .thenReturn(Optional.empty());
    }

    private ClassMember member(Long userId, ClassMemberRole role, ClassMemberStatus status) {
        return ClassMember.builder()
                .classroomId(CLASSROOM_ID)
                .userId(userId)
                .memberRole(role)
                .memberStatus(status)
                .build();
    }

    private void mockTeacherAccess(Long userId, boolean isTeacher) {
        when(classroomRepository.existsByClassroomIdAndTeacherId(CLASSROOM_ID, userId))
                .thenReturn(isTeacher);
    }

    private void mockMemberAccess(Long userId, ClassMemberRole role, ClassMemberStatus status) {
        when(classMemberRepository.findByClassroomIdAndUserId(CLASSROOM_ID, userId))
                .thenReturn(Optional.of(member(userId, role, status)));
    }

    private void mockNoMemberAccess(Long userId) {
        when(classMemberRepository.findByClassroomIdAndUserId(CLASSROOM_ID, userId))
                .thenReturn(Optional.empty());
    }

    private void mockStudentOwner() {
        mockMemberAccess(COMMENT_OWNER_ID, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE);
    }

    private Comment comment(Long commentId, Long userId, String content) {
        return Comment.builder()
                .commentId(commentId)
                .announcementId(ANNOUNCEMENT_ID)
                .userId(userId)
                .user(user(userId))
                .content(content)
                .isDeleted(false)
                .build();
    }

    private Comment saveExistingComment(Long commentId, Long userId, String content) {
        Comment comment = comment(commentId, userId, content);
        commentStore.put(commentId, comment);
        return comment;
    }

    private CommentCreateRequest createRequest(String content) {
        return CommentCreateRequest.builder()
                .content(content)
                .build();
    }

    private CommentUpdateRequest updateRequest(Long commentId, String content) {
        return CommentUpdateRequest.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }

    private CommentDeleteRequest deleteRequest(Long commentId) {
        CommentDeleteRequest request = new CommentDeleteRequest();
        request.setCommentId(commentId);
        return request;
    }

    private CommentListRequest listRequest() {
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");

        CommentListRequest request = new CommentListRequest();
        request.setAnnouncementId(ANNOUNCEMENT_ID);
        request.setPagination(pagination);

        return request;
    }

    private void mockActiveMemberAccess(Long userId) {
        mockTeacherAccess(userId, false);
        mockMemberAccess(userId, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE);
    }

    private void mockAssistantAccess(Long userId) {
        mockTeacherAccess(userId, false);
        mockMemberAccess(userId, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
    }

    private void assertCommentNotSaved() {
        verify(commentRepository, never()).saveAndFlush(any(Comment.class));
    }

    private void assertCommentNotDeleted(Long commentId) {
        verify(commentRepository, never()).softDeleteById(commentId);
    }

    private void assertSoftDeleted(Long commentId) {
        assertTrue(commentStore.get(commentId).getIsDeleted());
        verify(commentRepository).softDeleteById(commentId);
    }

    /**
     * Các bài kiểm thử cho chức năng tạo bình luận.
     */
    @Nested
    class CreateCommentTests {

        @Test
        @DisplayName("LH_BL_01 - Đảm bảo tạo bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void createComment_Success_WhenUserIsActiveMember() {
            // Given: Người dùng đã đăng nhập, thông báo cho phép bình luận và người dùng là
            // thành viên lớp đang hoạt động
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(USER_ID);

            // When: Người dùng thực hiện gửi bình luận với nội dung "Hello class"
            CommentResponse response = service.createComment(
                    ANNOUNCEMENT_ID,
                    createRequest("Hello class"));

            // Then: Kiểm tra phản hồi trả về có đầy đủ thông tin và quyền chỉnh sửa/xóa
            assertNotNull(response);
            assertEquals(ANNOUNCEMENT_ID, response.getAnnouncementId());
            assertEquals(USER_ID, response.getUserId());
            assertEquals("Hello class", response.getContent());
            assertTrue(response.getCanEdit());
            assertTrue(response.getCanDelete());

            // Đảm bảo bình luận đã được lưu vào repository với thông tin chính xác
            Comment saved = commentStore.values().stream().findFirst().orElseThrow();
            assertEquals("Hello class", saved.getContent());
            assertEquals(USER_ID, saved.getUserId());
            assertFalse(saved.getIsDeleted());
        }

        @Test
        @DisplayName("LH_BL_02 - Đảm bảo tạo bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void createComment_Success_WhenUserIsTeacher() {
            // Given: Người dùng là giáo viên của lớp học
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, true);

            // When: Giáo viên gửi bình luận
            CommentResponse response = service.createComment(
                    ANNOUNCEMENT_ID,
                    createRequest("Teacher comment"));

            // Then: Bình luận được lưu thành công
            assertNotNull(response);
            assertEquals(USER_ID, response.getUserId());
            assertEquals("Teacher comment", response.getContent());
        }

        @Test
        @DisplayName("LH_BL_03 - Đảm bảo tạo bình luận xử lý đúng trường hợp lỗi: fail_throws when announcement missing.")
        void createComment_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(USER_ID);
            mockAnnouncementMissing();

            assertThrows(AppException.class, () -> service.createComment(ANNOUNCEMENT_ID, createRequest("Hello")));

            assertCommentNotSaved();
        }

        @Test
        @DisplayName("LH_BL_04 - Đảm bảo tạo bình luận xử lý đúng trường hợp lỗi: fail_throws when user is not class member.")
        void createComment_Fail_ThrowsWhenUserIsNotClassMember() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockNoMemberAccess(USER_ID);

            assertThrows(AppException.class, () -> service.createComment(ANNOUNCEMENT_ID, createRequest("Hello")));

            assertCommentNotSaved();
        }

        @Test
        @DisplayName("LH_BL_05 - Đảm bảo tạo bình luận xử lý đúng trường hợp lỗi: fail_throws when member is not active.")
        void createComment_Fail_ThrowsWhenMemberIsNotActive() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockMemberAccess(USER_ID, ClassMemberRole.STUDENT, ClassMemberStatus.INACTIVE);

            assertThrows(AppException.class, () -> service.createComment(ANNOUNCEMENT_ID, createRequest("Hello")));

            assertCommentNotSaved();
        }

        @Test
        @DisplayName("LH_BL_06 - Đảm bảo tạo bình luận xử lý đúng trường hợp lỗi: fail_throws when announcement disables comments.")
        void createComment_Fail_ThrowsWhenAnnouncementDisablesComments() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(false);
            mockTeacherAccess(USER_ID, true);

            assertThrows(AppException.class, () -> service.createComment(ANNOUNCEMENT_ID, createRequest("Blocked")));

            assertCommentNotSaved();
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy danh sách bình luận.
     */
    @Nested
    class GetCommentListTests {

        @Test
        @DisplayName("LH_BL_07 - Đảm bảo xem danh sách bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void getCommentList_Success_ReturnsCommentsForActiveMember() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(USER_ID);

            Comment first = comment(1L, USER_ID, "First");
            Comment second = comment(2L, OTHER_USER_ID, "Second");

            when(commentRepository.findByAnnouncementIdAndNotDeleted(
                    eq(ANNOUNCEMENT_ID),
                    any(Pageable.class))).thenReturn(new PageImpl<>(List.of(first, second)));

            ResponseListData<CommentResponse> result = service.getCommentList(listRequest());

            assertNotNull(result);
            assertNotNull(result.getContent());
            List<CommentResponse> items = new ArrayList<>(result.getContent());
            assertEquals(2, items.size());
            assertEquals("First", items.get(0).getContent());
            assertEquals("Second", items.get(1).getContent());

            verify(commentRepository).findByAnnouncementIdAndNotDeleted(
                    eq(ANNOUNCEMENT_ID),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("LH_BL_08 - Đảm bảo xem danh sách bình luận xử lý đúng trường hợp lỗi: fail_throws when announcement missing.")
        void getCommentList_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(USER_ID);
            mockAnnouncementMissing();

            assertThrows(AppException.class, () -> service.getCommentList(listRequest()));

            verify(commentRepository, never()).findByAnnouncementIdAndNotDeleted(
                    anyLong(),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("LH_BL_09 - Đảm bảo xem danh sách bình luận xử lý đúng trường hợp lỗi: fail_throws when user has no classroom access.")
        void getCommentList_Fail_ThrowsWhenUserHasNoClassroomAccess() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockNoMemberAccess(USER_ID);

            assertThrows(AppException.class, () -> service.getCommentList(listRequest()));

            verify(commentRepository, never()).findByAnnouncementIdAndNotDeleted(
                    anyLong(),
                    any(Pageable.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật bình luận.
     */
    @Nested
    class UpdateCommentTests {

        @Test
        @DisplayName("LH_BL_10 - Đảm bảo cập nhật bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void updateComment_Success_UpdatesOwnComment() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            saveExistingComment(5L, USER_ID, "Old");

            CommentResponse response = service.updateComment(updateRequest(5L, "Updated"));

            Comment updated = commentRepository.findByCommentIdAndNotDeleted(5L).orElseThrow();

            assertNotNull(response);
            assertEquals("Updated", updated.getContent());
            assertEquals("Updated", response.getContent());
            assertTrue(response.getCanEdit());
            assertTrue(response.getCanDelete());
        }

        @Test
        @DisplayName("LH_BL_11 - Đảm bảo cập nhật bình luận xử lý đúng trường hợp lỗi: fail_throws when comment missing.")
        void updateComment_Fail_ThrowsWhenCommentMissing() {
            mockCurrentUser(USER_ID);

            assertThrows(AppException.class, () -> service.updateComment(updateRequest(99L, "Updated")));

            assertCommentNotSaved();
        }

        @Test
        @DisplayName("LH_BL_12 - Đảm bảo cập nhật bình luận xử lý đúng trường hợp lỗi: fail_throws when other user tries to edit.")
        void updateComment_Fail_ThrowsWhenOtherUserTriesToEdit() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(OTHER_USER_ID);
            saveExistingComment(5L, COMMENT_OWNER_ID, "Old");

            assertThrows(AppException.class, () -> service.updateComment(updateRequest(5L, "Hack")));

            assertEquals("Old", commentStore.get(5L).getContent());
        }

        @Test
        @DisplayName("LH_BL_13 - Đảm bảo cập nhật bình luận xử lý đúng trường hợp lỗi: fail_throws when teacher tries to edit student comment.")
        void updateComment_Fail_ThrowsWhenTeacherTriesToEditStudentComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(OTHER_USER_ID, true);
            saveExistingComment(5L, COMMENT_OWNER_ID, "Old");

            assertThrows(AppException.class, () -> service.updateComment(updateRequest(5L, "Teacher edit")));

            assertEquals("Old", commentStore.get(5L).getContent());
        }
    }

    /**
     * Các bài kiểm thử cho chức năng xóa bình luận.
     */
    @Nested
    class DeleteCommentTests {

        @Test
        @DisplayName("LH_BL_14 - Đảm bảo xóa bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void deleteComment_Success_SoftDeletesOwnComment() {
            // Given: Người dùng đã đăng nhập và có 1 bình luận ID 6
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            saveExistingComment(6L, USER_ID, "Delete");

            // When: Thực hiện xóa chính bình luận của mình
            service.deleteComment(deleteRequest(6L));

            // Then: Bình luận phải được đánh dấu xóa (soft delete)
            assertSoftDeleted(6L);
        }

        @Test
        @DisplayName("LH_BL_15 - Đảm bảo xóa bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void deleteComment_Success_TeacherDeletesStudentComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(OTHER_USER_ID, true);
            saveExistingComment(6L, COMMENT_OWNER_ID, "Student comment");

            service.deleteComment(deleteRequest(6L));

            assertSoftDeleted(6L);
        }

        @Test
        @DisplayName("LH_BL_16 - Đảm bảo xóa bình luận hoạt động đúng với dữ liệu mock hợp lệ và trả/lưu kết quả theo kỳ vọng.")
        void deleteComment_Success_AssistantDeletesStudentComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockAssistantAccess(OTHER_USER_ID);
            mockStudentOwner();
            saveExistingComment(6L, COMMENT_OWNER_ID, "Student comment");

            service.deleteComment(deleteRequest(6L));

            assertSoftDeleted(6L);
        }

        @Test
        @DisplayName("LH_BL_17 - Đảm bảo xóa bình luận xử lý đúng trường hợp lỗi: fail_throws when comment missing.")
        void deleteComment_Fail_ThrowsWhenCommentMissing() {
            mockCurrentUser(USER_ID);

            assertThrows(AppException.class, () -> service.deleteComment(deleteRequest(99L)));

            verify(commentRepository, never()).softDeleteById(anyLong());
        }

        @Test
        @DisplayName("LH_BL_18 - Đảm bảo xóa bình luận xử lý đúng trường hợp lỗi: fail_throws when current user cannot delete others comment.")
        void deleteComment_Fail_ThrowsWhenCurrentUserCannotDeleteOthersComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(OTHER_USER_ID);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Other user");

            assertThrows(AppException.class, () -> service.deleteComment(deleteRequest(9L)));

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }

        @Test
        @DisplayName("LH_BL_19 - Đảm bảo xóa bình luận xử lý đúng trường hợp lỗi: fail_throws when assistant deletes teacher comment.")
        void deleteComment_Fail_ThrowsWhenAssistantDeletesTeacherComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockAssistantAccess(OTHER_USER_ID);

            mockMemberAccess(COMMENT_OWNER_ID, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Teacher comment");

            assertThrows(AppException.class, () -> service.deleteComment(deleteRequest(9L)));

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }

        @Test
        @DisplayName("LH_BL_20 - Đảm bảo xóa bình luận xử lý đúng trường hợp lỗi: fail_throws when assistant deletes assistant comment.")
        void deleteComment_Fail_ThrowsWhenAssistantDeletesAssistantComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockAssistantAccess(OTHER_USER_ID);

            mockMemberAccess(COMMENT_OWNER_ID, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Assistant comment");

            assertThrows(AppException.class, () -> service.deleteComment(deleteRequest(9L)));

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }
    }
}