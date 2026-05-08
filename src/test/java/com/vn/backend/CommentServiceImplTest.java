package com.vn.backend;

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
import com.vn.backend.repositories.CommentRepository;
import com.vn.backend.repositories.ClassroomRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {

    private static final Long USER_ID = 4L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long COMMENT_OWNER_ID = 4L;
    private static final Long ANNOUNCEMENT_ID = 10L;
    private static final Long CLASSROOM_ID = 20L;

    @Mock private CommentRepository commentRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private AuthService authService;

    private CommentServiceImpl service;

    private final Map<Long, Comment> commentStore = new HashMap<>();
    private final AtomicLong commentIds = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new CommentServiceImpl(
                commentRepository,
                announcementRepository,
                classMemberRepository,
                classroomRepository,
                authService,
                messageUtils
        );

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

    @Nested
    class CreateCommentTests {

        @Test
        void createComment_Success_WhenUserIsActiveMember() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(USER_ID);

            CommentResponse response = service.createComment(
                    ANNOUNCEMENT_ID,
                    createRequest("Hello class")
            );

            assertNotNull(response);
            assertEquals(ANNOUNCEMENT_ID, response.getAnnouncementId());
            assertEquals(USER_ID, response.getUserId());
            assertEquals("Hello class", response.getContent());
            assertTrue(response.getCanEdit());
            assertTrue(response.getCanDelete());

            Comment saved = commentStore.values().stream().findFirst().orElseThrow();
            assertEquals("Hello class", saved.getContent());
            assertEquals(USER_ID, saved.getUserId());
            assertFalse(saved.getIsDeleted());
        }

        @Test
        void createComment_Success_WhenUserIsTeacher() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, true);

            CommentResponse response = service.createComment(
                    ANNOUNCEMENT_ID,
                    createRequest("Teacher comment")
            );

            assertNotNull(response);
            assertEquals(USER_ID, response.getUserId());
            assertEquals("Teacher comment", response.getContent());
        }

        @Test
        void createComment_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(USER_ID);
            mockAnnouncementMissing();

            assertThrows(AppException.class, () ->
                    service.createComment(ANNOUNCEMENT_ID, createRequest("Hello"))
            );

            assertCommentNotSaved();
        }

        @Test
        void createComment_Fail_ThrowsWhenUserIsNotClassMember() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockNoMemberAccess(USER_ID);

            assertThrows(AppException.class, () ->
                    service.createComment(ANNOUNCEMENT_ID, createRequest("Hello"))
            );

            assertCommentNotSaved();
        }

        @Test
        void createComment_Fail_ThrowsWhenMemberIsNotActive() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockMemberAccess(USER_ID, ClassMemberRole.STUDENT, ClassMemberStatus.INACTIVE);

            assertThrows(AppException.class, () ->
                    service.createComment(ANNOUNCEMENT_ID, createRequest("Hello"))
            );

            assertCommentNotSaved();
        }

        @Test
        void createComment_Fail_ThrowsWhenAnnouncementDisablesComments() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(false);
            mockTeacherAccess(USER_ID, true);

            assertThrows(AppException.class, () ->
                    service.createComment(ANNOUNCEMENT_ID, createRequest("Blocked"))
            );

            assertCommentNotSaved();
        }
    }

    @Nested
    class GetCommentListTests {

        @Test
        void getCommentList_Success_ReturnsCommentsForActiveMember() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(USER_ID);

            Comment first = comment(1L, USER_ID, "First");
            Comment second = comment(2L, OTHER_USER_ID, "Second");

            when(commentRepository.findByAnnouncementIdAndNotDeleted(
                    eq(ANNOUNCEMENT_ID),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(first, second)));

            ResponseListData<CommentResponse> result = service.getCommentList(listRequest());

            assertNotNull(result);
            assertNotNull(result.getContent());
            List<CommentResponse> items = new ArrayList<>(result.getContent());
            assertEquals(2, items.size());
            assertEquals("First", items.get(0).getContent());
            assertEquals("Second", items.get(1).getContent());

            verify(commentRepository).findByAnnouncementIdAndNotDeleted(
                    eq(ANNOUNCEMENT_ID),
                    any(Pageable.class)
            );
        }

        @Test
        void getCommentList_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(USER_ID);
            mockAnnouncementMissing();

            assertThrows(AppException.class, () -> service.getCommentList(listRequest()));

            verify(commentRepository, never()).findByAnnouncementIdAndNotDeleted(
                    anyLong(),
                    any(Pageable.class)
            );
        }

        @Test
        void getCommentList_Fail_ThrowsWhenUserHasNoClassroomAccess() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(USER_ID, false);
            mockNoMemberAccess(USER_ID);

            assertThrows(AppException.class, () -> service.getCommentList(listRequest()));

            verify(commentRepository, never()).findByAnnouncementIdAndNotDeleted(
                    anyLong(),
                    any(Pageable.class)
            );
        }
    }

    @Nested
    class UpdateCommentTests {

        @Test
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
        void updateComment_Fail_ThrowsWhenCommentMissing() {
            mockCurrentUser(USER_ID);

            assertThrows(AppException.class, () ->
                    service.updateComment(updateRequest(99L, "Updated"))
            );

            assertCommentNotSaved();
        }

        @Test
        void updateComment_Fail_ThrowsWhenOtherUserTriesToEdit() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(OTHER_USER_ID);
            saveExistingComment(5L, COMMENT_OWNER_ID, "Old");

            assertThrows(AppException.class, () ->
                    service.updateComment(updateRequest(5L, "Hack"))
            );

            assertEquals("Old", commentStore.get(5L).getContent());
        }

        @Test
        void updateComment_Fail_ThrowsWhenTeacherTriesToEditStudentComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(OTHER_USER_ID, true);
            saveExistingComment(5L, COMMENT_OWNER_ID, "Old");

            assertThrows(AppException.class, () ->
                    service.updateComment(updateRequest(5L, "Teacher edit"))
            );

            assertEquals("Old", commentStore.get(5L).getContent());
        }
    }

    @Nested
    class DeleteCommentTests {

        @Test
        void deleteComment_Success_SoftDeletesOwnComment() {
            mockCurrentUser(USER_ID);
            mockAnnouncement(true);
            saveExistingComment(6L, USER_ID, "Delete");

            service.deleteComment(deleteRequest(6L));

            assertSoftDeleted(6L);
        }

        @Test
        void deleteComment_Success_TeacherDeletesStudentComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockTeacherAccess(OTHER_USER_ID, true);
            saveExistingComment(6L, COMMENT_OWNER_ID, "Student comment");

            service.deleteComment(deleteRequest(6L));

            assertSoftDeleted(6L);
        }

        @Test
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
        void deleteComment_Fail_ThrowsWhenCommentMissing() {
            mockCurrentUser(USER_ID);

            assertThrows(AppException.class, () -> service.deleteComment(deleteRequest(99L)));

            verify(commentRepository, never()).softDeleteById(anyLong());
        }

        @Test
        void deleteComment_Fail_ThrowsWhenCurrentUserCannotDeleteOthersComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockActiveMemberAccess(OTHER_USER_ID);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Other user");

            assertThrows(AppException.class, () ->
                    service.deleteComment(deleteRequest(9L))
            );

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }

        @Test
        void deleteComment_Fail_ThrowsWhenAssistantDeletesTeacherComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockAssistantAccess(OTHER_USER_ID);

            mockMemberAccess(COMMENT_OWNER_ID, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Teacher comment");

            assertThrows(AppException.class, () ->
                    service.deleteComment(deleteRequest(9L))
            );

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }

        @Test
        void deleteComment_Fail_ThrowsWhenAssistantDeletesAssistantComment() {
            mockCurrentUser(OTHER_USER_ID);
            mockAnnouncement(true);
            mockAssistantAccess(OTHER_USER_ID);

            mockMemberAccess(COMMENT_OWNER_ID, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
            saveExistingComment(9L, COMMENT_OWNER_ID, "Assistant comment");

            assertThrows(AppException.class, () ->
                    service.deleteComment(deleteRequest(9L))
            );

            assertFalse(commentStore.get(9L).getIsDeleted());
            assertCommentNotDeleted(9L);
        }
    }
}