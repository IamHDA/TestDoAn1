package com.vn.backend.unit;

import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementFilterRequest;
import com.vn.backend.dto.request.announcement.AnnouncementListRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.impl.AnnouncementServiceImpl;
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
 * Lớp kiểm thử cho AnnouncementServiceImpl, tập trung vào các unit test cho
 * chức năng thông báo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementServiceImplTest {

    private static final Long CLASSROOM_ID = 10L;
    private static final Long TEACHER_ID = 4L;
    private static final Long STUDENT_ID = 8L;
    private static final Long ASSISTANT_ID = 9L;
    private static final Long ANNOUNCEMENT_ID = 11L;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ClassroomSettingRepository classroomSettingRepository;

    private AnnouncementServiceImpl service;

    private final Map<Long, Announcement> announcementStore = new HashMap<>();
    private final Map<Long, List<Attachment>> attachmentStore = new HashMap<>();

    private final AtomicLong announcementIds = new AtomicLong(1);
    private final AtomicLong attachmentIds = new AtomicLong(1);

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử.
     */
    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new AnnouncementServiceImpl(
                messageUtils,
                announcementRepository,
                attachmentRepository,
                classMemberRepository,
                classroomRepository,
                authService,
                notificationService,
                classroomSettingRepository);

        mockAnnouncementRepositoryStorage();
        mockAttachmentRepositoryStorage();
    }

    /**
     * Giả lập lưu trữ AnnouncementRepository trong bộ nhớ.
     */
    private void mockAnnouncementRepositoryStorage() {
        when(announcementRepository.saveAndFlush(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);

            if (announcement.getAnnouncementId() == null) {
                announcement.setAnnouncementId(announcementIds.getAndIncrement());
            }

            announcementStore.put(announcement.getAnnouncementId(), announcement);
            return announcement;
        });

        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcementStore.put(announcement.getAnnouncementId(), announcement);
            return announcement;
        });

        when(announcementRepository.findByIdAndNotDeleted(anyLong())).thenAnswer(invocation -> {
            Long announcementId = invocation.getArgument(0);
            Announcement announcement = announcementStore.get(announcementId);

            if (announcement == null || Boolean.TRUE.equals(announcement.getIsDeleted())) {
                return Optional.empty();
            }

            return Optional.of(announcement);
        });
    }

    /**
     * Giả lập lưu trữ AttachmentRepository trong bộ nhớ.
     */
    private void mockAttachmentRepositoryStorage() {
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);

            if (attachment.getAttachmentId() == null) {
                attachment.setAttachmentId(attachmentIds.getAndIncrement());
            }

            attachmentStore.computeIfAbsent(attachment.getObjectId(), key -> new ArrayList<>()).add(attachment);
            return attachment;
        });

        when(attachmentRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Attachment> attachments = invocation.getArgument(0);

            for (Attachment attachment : attachments) {
                if (attachment.getAttachmentId() == null) {
                    attachment.setAttachmentId(attachmentIds.getAndIncrement());
                }

                attachmentStore.computeIfAbsent(attachment.getObjectId(), key -> new ArrayList<>()).add(attachment);
            }

            return attachments;
        });

        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(
                anyLong(),
                any(AttachmentType.class))).thenAnswer(invocation -> {
                    Long objectId = invocation.getArgument(0);

                    return attachmentStore.getOrDefault(objectId, List.of())
                            .stream()
                            .filter(attachment -> !Boolean.TRUE.equals(attachment.getIsDeleted()))
                            .toList();
                });

        doAnswer(invocation -> {
            Long attachmentId = invocation.getArgument(0);

            attachmentStore.values()
                    .stream()
                    .flatMap(List::stream)
                    .filter(attachment -> attachmentId.equals(attachment.getAttachmentId()))
                    .findFirst()
                    .ifPresent(attachment -> attachment.setIsDeleted(true));

            return null;
        }).when(attachmentRepository).softDeleteById(anyLong());
    }

    private User user(Long userId, Role role) {
        return User.builder()
                .id(userId)
                .username("user" + userId)
                .fullName("User " + userId)
                .avatarUrl("avatar-" + userId + ".png")
                .role(role)
                .build();
    }

    private void mockCurrentUser(Long userId, Role role) {
        when(authService.getCurrentUser()).thenReturn(user(userId, role));
    }

    private Classroom classroom() {
        return Classroom.builder()
                .classroomId(CLASSROOM_ID)
                .className("SE Class")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .teacherId(TEACHER_ID)
                .build();
    }

    private void mockClassroomExists() {
        when(classroomRepository.findByClassroomIdAndClassroomStatus(
                CLASSROOM_ID,
                ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom()));
    }

    private void mockClassroomMissing() {
        when(classroomRepository.findByClassroomIdAndClassroomStatus(
                CLASSROOM_ID,
                ClassroomStatus.ACTIVE)).thenReturn(Optional.empty());
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

    private void mockStudentAccess(Long userId) {
        mockTeacherAccess(userId, false);
        mockMemberAccess(userId, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE);
    }

    private void mockAssistantAccess(Long userId) {
        mockTeacherAccess(userId, false);
        mockMemberAccess(userId, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
    }

    private void mockPostAllowed() {
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(CLASSROOM_ID))
                .thenReturn(false);
    }

    private void mockPostDisabled() {
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(CLASSROOM_ID))
                .thenReturn(true);
    }

    private Announcement announcement(
            Long announcementId,
            Long createdBy,
            AnnouncementType type) {
        return Announcement.builder()
                .announcementId(announcementId)
                .classroomId(CLASSROOM_ID)
                .createdBy(createdBy)
                .createdByUser(user(createdBy, createdBy.equals(TEACHER_ID) ? Role.TEACHER : Role.STUDENT))
                .title("Old title")
                .content("Old content")
                .type(type)
                .allowComments(true)
                .objectId(null)
                .isDeleted(false)
                .build();
    }

    private Announcement saveAnnouncement(Long announcementId, Long createdBy, AnnouncementType type) {
        Announcement announcement = announcement(announcementId, createdBy, type);
        announcementStore.put(announcementId, announcement);
        return announcement;
    }

    private Attachment attachment(
            Long attachmentId,
            Long objectId,
            String fileName,
            boolean deleted) {
        return Attachment.builder()
                .attachmentId(attachmentId)
                .objectId(objectId)
                .attachmentType(AttachmentType.GENERIC)
                .fileName(fileName)
                .fileUrl("https://cdn/" + fileName)
                .description("Description " + fileName)
                .uploadedBy(TEACHER_ID)
                .isDeleted(deleted)
                .build();
    }

    private void saveAttachment(Long announcementId, Attachment attachment) {
        attachmentStore.computeIfAbsent(announcementId, key -> new ArrayList<>()).add(attachment);
    }

    private AnnouncementCreateRequest createRequest(
            AnnouncementType type,
            String title,
            String content,
            boolean allowComments) {
        return AnnouncementCreateRequest.builder()
                .title(title)
                .content(content)
                .type(type)
                .allowComments(allowComments)
                .build();
    }

    private AnnouncementCreateRequest createRequestWithAttachment() {
        return AnnouncementCreateRequest.builder()
                .title("Week 1")
                .content("Welcome")
                .type(AnnouncementType.GENERIC)
                .allowComments(true)
                .attachments(List.of(
                        AnnouncementCreateRequest.AttachmentRequest.builder()
                                .fileName("slide.pdf")
                                .fileUrl("https://cdn/slide.pdf")
                                .description("Deck")
                                .build()))
                .build();
    }

    private AnnouncementUpdateRequest updateRequest(
            String title,
            String content,
            Boolean allowComments) {
        return AnnouncementUpdateRequest.builder()
                .title(title)
                .content(content)
                .allowComments(allowComments)
                .build();
    }

    private AnnouncementUpdateRequest updateRequestWithAttachments(
            List<AnnouncementUpdateRequest.AttachmentUpdateRequest> attachments) {
        return AnnouncementUpdateRequest.builder()
                .title("Updated title")
                .content("Updated content")
                .allowComments(false)
                .attachments(attachments)
                .build();
    }

    private AnnouncementUpdateRequest.AttachmentUpdateRequest existingAttachment(Long attachmentId) {
        return AnnouncementUpdateRequest.AttachmentUpdateRequest.builder()
                .attachmentId(attachmentId)
                .build();
    }

    private AnnouncementUpdateRequest.AttachmentUpdateRequest newAttachment(String fileName) {
        return AnnouncementUpdateRequest.AttachmentUpdateRequest.builder()
                .fileName(fileName)
                .fileUrl("https://cdn/" + fileName)
                .description("New " + fileName)
                .build();
    }

    private AnnouncementListRequest listRequest() {
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");

        AnnouncementListRequest request = new AnnouncementListRequest();
        request.setPagination(pagination);
        return request;
    }

    private AnnouncementListRequest listRequestWithFilter(AnnouncementType type) {
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");

        AnnouncementFilterRequest filter = new AnnouncementFilterRequest();
        filter.setType(type);

        AnnouncementListRequest request = new AnnouncementListRequest();
        request.setPagination(pagination);
        request.setFilters(filter);
        return request;
    }

    private void assertAnnouncementNotSaved(Announcement announcement) {
        verify(announcementRepository, never()).save(eq(announcement));
    }

    private void assertNotificationNotSent() {
        verify(notificationService, never())
                .createNotificationForClass(any(), anyLong(), any(), anyLong());
    }

    /**
     * Các bài kiểm thử cho chức năng tạo thông báo.
     */
    @Nested
    class CreateAnnouncementTests {

        @Test
        void createAnnouncement_Success_PersistsAnnouncementAndAttachments() {
            // Given: Thiết lập ngữ cảnh - người dùng là giáo viên, lớp học tồn tại, có
            // quyền truy cập và được phép đăng bài
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomExists();
            mockTeacherAccess(TEACHER_ID, true);
            mockPostAllowed();

            // When: Thực hiện hành động - giáo viên tạo thông báo có kèm tệp đính kèm
            service.createAnnouncement(CLASSROOM_ID, createRequestWithAttachment());

            // Then: Kiểm tra kết quả - thông báo và tệp đính kèm được lưu đúng thông tin
            Announcement saved = announcementStore.values().stream().findFirst().orElseThrow();

            assertEquals("Week 1", saved.getTitle());
            assertEquals("Welcome", saved.getContent());
            assertEquals(AnnouncementType.GENERIC, saved.getType());
            assertEquals(TEACHER_ID, saved.getCreatedBy());
            assertFalse(saved.getIsDeleted());

            assertEquals(1, attachmentStore.get(saved.getAnnouncementId()).size());
            assertEquals("slide.pdf", attachmentStore.get(saved.getAnnouncementId()).get(0).getFileName());

            // Đảm bảo thông báo (notification) đã được gửi đến cả lớp
            verify(notificationService).createNotificationForClass(
                    any(User.class),
                    eq(CLASSROOM_ID),
                    eq(NotificationObjectType.ANNOUNCEMENT),
                    eq(saved.getAnnouncementId()));
        }

        @Test
        void createAnnouncement_Success_StudentCreatesGenericWhenPostAllowed() {
            // Given: Người dùng là sinh viên và cấu hình lớp học cho phép sinh viên đăng bài
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomExists();
            mockStudentAccess(STUDENT_ID);
            mockPostAllowed();

            // When: Sinh viên thực hiện tạo thông báo loại GENERIC
            service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.GENERIC, "Question", "Can I ask?", true));

            // Then: Thông báo được lưu thành công với người tạo là sinh viên
            Announcement saved = announcementStore.values().stream().findFirst().orElseThrow();

            assertEquals("Question", saved.getTitle());
            assertEquals(STUDENT_ID, saved.getCreatedBy());
        }

        @Test
        void createAnnouncement_Fail_ThrowsWhenClassroomMissing() {
            // Given: Người dùng là giáo viên nhưng lớp học không tồn tại
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomMissing();

            // When & Then: Phải ném ra AppException khi cố gắng tạo thông báo
            assertThrows(AppException.class, () -> service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.GENERIC, "Title", "Content", true)));

            // Kiểm tra: Không có thông báo nào được lưu và không có notification được gửi
            verify(announcementRepository, never()).saveAndFlush(any(Announcement.class));
            assertNotificationNotSent();
        }

        @Test
        void createAnnouncement_Fail_ThrowsWhenUserHasNoClassroomAccess() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomExists();
            mockTeacherAccess(STUDENT_ID, false);
            mockNoMemberAccess(STUDENT_ID);
            mockPostAllowed();

            assertThrows(AppException.class, () -> service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.GENERIC, "Title", "Content", true)));

            verify(announcementRepository, never()).saveAndFlush(any(Announcement.class));
            assertNotificationNotSent();
        }

        @Test
        void createAnnouncement_Fail_ThrowsWhenMemberInactive() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomExists();
            mockTeacherAccess(STUDENT_ID, false);
            mockMemberAccess(STUDENT_ID, ClassMemberRole.STUDENT, ClassMemberStatus.INACTIVE);
            mockPostAllowed();

            assertThrows(AppException.class, () -> service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.GENERIC, "Title", "Content", true)));

            verify(announcementRepository, never()).saveAndFlush(any(Announcement.class));
            assertNotificationNotSent();
        }

        @Test
        void createAnnouncement_Fail_ThrowsWhenStudentPostDisabled() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomExists();
            mockStudentAccess(STUDENT_ID);
            mockPostDisabled();

            assertThrows(AppException.class, () -> service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.GENERIC, "Title", "Content", true)));

            verify(announcementRepository, never()).saveAndFlush(any(Announcement.class));
            assertNotificationNotSent();
        }

        @Test
        void createAnnouncement_Fail_ThrowsWhenStudentPostsMaterial() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomExists();
            mockStudentAccess(STUDENT_ID);
            mockPostAllowed();

            assertThrows(AppException.class, () -> service.createAnnouncement(
                    CLASSROOM_ID,
                    createRequest(AnnouncementType.MATERIAL, "Material", "Private material", true)));

            verify(announcementRepository, never()).saveAndFlush(any(Announcement.class));
            assertNotificationNotSent();
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy danh sách thông báo.
     */
    @Nested
    class GetAnnouncementListTests {

        @Test
        void getAnnouncementList_Success_ReturnsAnnouncementsForTeacher() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            Announcement first = announcement(1L, TEACHER_ID, AnnouncementType.GENERIC);
            Announcement second = announcement(2L, STUDENT_ID, AnnouncementType.GENERIC);

            when(announcementRepository.findByClassroomIdWithFilters(
                    eq(CLASSROOM_ID),
                    eq(null),
                    any(Pageable.class))).thenReturn(new PageImpl<>(List.of(first, second)));

            ResponseListData<AnnouncementResponse> result = service.getAnnouncementList(CLASSROOM_ID, listRequest());

            assertNotNull(result);
            assertNotNull(result.getContent());
            List<AnnouncementResponse> items = new ArrayList<>(result.getContent());
            assertEquals(2, items.size());
            assertTrue(items.get(0).getCanEdit());
            assertTrue(items.get(0).getCanDelete());

            verify(announcementRepository).findByClassroomIdWithFilters(
                    eq(CLASSROOM_ID),
                    eq(null),
                    any(Pageable.class));
        }

        @Test
        void getAnnouncementList_Success_UsesFilterType() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);

            Announcement announcement = announcement(1L, TEACHER_ID, AnnouncementType.MATERIAL);

            when(announcementRepository.findByClassroomIdWithFilters(
                    eq(CLASSROOM_ID),
                    eq(AnnouncementType.MATERIAL),
                    any(Pageable.class))).thenReturn(new PageImpl<>(List.of(announcement)));

            ResponseListData<AnnouncementResponse> result = service.getAnnouncementList(CLASSROOM_ID,
                    listRequestWithFilter(AnnouncementType.MATERIAL));

            assertEquals(1, result.getContent().size());

            verify(announcementRepository).findByClassroomIdWithFilters(
                    eq(CLASSROOM_ID),
                    eq(AnnouncementType.MATERIAL),
                    any(Pageable.class));
        }

        @Test
        void getAnnouncementList_Fail_ThrowsWhenUserHasNoClassroomAccess() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockTeacherAccess(STUDENT_ID, false);
            mockNoMemberAccess(STUDENT_ID);

            assertThrows(AppException.class, () -> service.getAnnouncementList(CLASSROOM_ID, listRequest()));

            verify(announcementRepository, never()).findByClassroomIdWithFilters(
                    anyLong(),
                    any(),
                    any(Pageable.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy chi tiết thông báo.
     */
    @Nested
    class GetAnnouncementDetailTests {

        @Test
        void getAnnouncementDetail_Success_ReturnsPermissionsForOwnerStudent() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);
            saveAttachment(ANNOUNCEMENT_ID, attachment(1L, ANNOUNCEMENT_ID, "guide.pdf", false));

            AnnouncementResponse response = service.getAnnouncementDetail(ANNOUNCEMENT_ID);

            assertNotNull(response);
            assertEquals(ANNOUNCEMENT_ID, response.getAnnouncementId());
            assertTrue(response.getCanEdit());
            assertTrue(response.getCanDelete());
            assertEquals(1, response.getAttachments().size());
            assertEquals("guide.pdf", response.getAttachments().get(0).getFileName());
        }

        @Test
        void getAnnouncementDetail_Success_TeacherCanEditAndDelete() {
            // Given: Người dùng là giáo viên của lớp học
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            // Có một thông báo do sinh viên tạo
            saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);

            // When: Lấy chi tiết thông báo
            AnnouncementResponse response = service.getAnnouncementDetail(ANNOUNCEMENT_ID);

            // Then: Giáo viên phải có quyền sửa và xóa thông báo đó (vì là chủ lớp học)
            assertTrue(response.getCanEdit());
            assertTrue(response.getCanDelete());
        }

        @Test
        void getAnnouncementDetail_Success_NonOwnerStudentCannotEditOrDelete() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);

            saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            AnnouncementResponse response = service.getAnnouncementDetail(ANNOUNCEMENT_ID);

            assertFalse(response.getCanEdit());
            assertFalse(response.getCanDelete());
        }

        @Test
        void getAnnouncementDetail_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);

            assertThrows(AppException.class, () -> service.getAnnouncementDetail(99L));
        }

        @Test
        void getAnnouncementDetail_Fail_ThrowsWhenUserHasNoClassroomAccess() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockTeacherAccess(STUDENT_ID, false);
            mockNoMemberAccess(STUDENT_ID);

            saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            assertThrows(AppException.class, () -> service.getAnnouncementDetail(ANNOUNCEMENT_ID));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật thông báo.
     */
    @Nested
    class UpdateAnnouncementTests {

        @Test
        void updateAnnouncement_Success_OwnerUpdatesAnnouncement() {
            // Given: Người dùng là sinh viên đã tạo một thông báo GENERIC
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);
            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);

            // When: Thực hiện cập nhật tiêu đề, nội dung và tắt cho phép bình luận
            service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequest("Updated title", "Updated content", false));

            // Then: Các trường thông tin của thông báo phải được thay đổi tương ứng
            assertEquals("Updated title", announcement.getTitle());
            assertEquals("Updated content", announcement.getContent());
            assertFalse(announcement.getAllowComments());

            // Đảm bảo thông báo đã cập nhật được lưu vào repository
            verify(announcementRepository).save(eq(announcement));
        }

        @Test
        void updateAnnouncement_Success_TeacherUpdatesAnnouncement() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);

            service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequest("Teacher title", "Teacher content", true));

            assertEquals("Teacher title", announcement.getTitle());
            assertEquals("Teacher content", announcement.getContent());
            assertTrue(announcement.getAllowComments());

            verify(announcementRepository).save(eq(announcement));
        }

        @Test
        void updateAnnouncement_Success_AddsNewAttachmentAndKeepsExistingAttachment() {
            // Given: Người dùng là giáo viên có quyền truy cập lớp học
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            // Đã có một thông báo với một tệp đính kèm hiện tại (old.pdf)
            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);
            Attachment oldAttachment = attachment(1L, ANNOUNCEMENT_ID, "old.pdf", false);
            saveAttachment(ANNOUNCEMENT_ID, oldAttachment);

            // When: Cập nhật thông báo, giữ lại tệp cũ (ID: 1) và thêm một tệp mới
            // (new.pdf)
            service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequestWithAttachments(List.of(
                            existingAttachment(1L),
                            newAttachment("new.pdf"))));

            // Then: Thông báo phải có tổng cộng 2 tệp đính kèm
            List<Attachment> attachments = attachmentStore.get(ANNOUNCEMENT_ID);

            assertEquals("Updated title", announcement.getTitle());
            assertEquals(2, attachments.size());
            // Tệp cũ không bị xóa
            assertFalse(oldAttachment.getIsDeleted());
            // Tệp mới đã có trong danh sách
            assertTrue(attachments.stream().anyMatch(att -> "new.pdf".equals(att.getFileName())));
        }

        @Test
        void updateAnnouncement_Success_DeletesRemovedAttachment() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            Attachment keep = attachment(1L, ANNOUNCEMENT_ID, "keep.pdf", false);
            Attachment remove = attachment(2L, ANNOUNCEMENT_ID, "remove.pdf", false);
            saveAttachment(ANNOUNCEMENT_ID, keep);
            saveAttachment(ANNOUNCEMENT_ID, remove);

            service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequestWithAttachments(List.of(existingAttachment(1L))));

            assertFalse(keep.getIsDeleted());
            assertTrue(remove.getIsDeleted());
            verify(attachmentRepository).softDeleteById(2L);
        }

        @Test
        void updateAnnouncement_Success_DeletesAllAttachmentsWhenRequestAttachmentsEmpty() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            Attachment first = attachment(1L, ANNOUNCEMENT_ID, "first.pdf", false);
            Attachment second = attachment(2L, ANNOUNCEMENT_ID, "second.pdf", false);
            saveAttachment(ANNOUNCEMENT_ID, first);
            saveAttachment(ANNOUNCEMENT_ID, second);

            service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequestWithAttachments(List.of()));

            assertTrue(first.getIsDeleted());
            assertTrue(second.getIsDeleted());
            verify(attachmentRepository).softDeleteById(1L);
            verify(attachmentRepository).softDeleteById(2L);
        }

        @Test
        void updateAnnouncement_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            assertThrows(AppException.class, () -> service.updateAnnouncement(
                    99L,
                    updateRequest("Updated", "Updated", true)));

            verify(announcementRepository, never()).save(any(Announcement.class));
        }

        @Test
        void updateAnnouncement_Fail_ThrowsWhenAssistantEditsOthersAnnouncement() {
            mockCurrentUser(ASSISTANT_ID, Role.STUDENT);
            mockAssistantAccess(ASSISTANT_ID);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            assertThrows(AppException.class, () -> service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequest("Updated", "Updated content", true)));

            assertAnnouncementNotSaved(announcement);
        }

        @Test
        void updateAnnouncement_Fail_ThrowsWhenStudentEditsOthersAnnouncement() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            assertThrows(AppException.class, () -> service.updateAnnouncement(
                    ANNOUNCEMENT_ID,
                    updateRequest("Updated", "Updated content", true)));

            assertAnnouncementNotSaved(announcement);
        }
    }

    /**
     * Các bài kiểm thử cho chức năng xóa thông báo.
     */
    @Nested
    class DeleteAnnouncementTests {

        @Test
        void deleteAnnouncement_Success_OwnerDeletesAnnouncement() {
            // Given: Người dùng là sinh viên đã tạo thông báo trước đó
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);
            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);

            // When: Thực hiện xóa thông báo
            service.deleteAnnouncement(ANNOUNCEMENT_ID);

            // Then: Trạng thái isDeleted chuyển thành true và được lưu lại
            assertTrue(announcement.getIsDeleted());
            verify(announcementRepository).save(eq(announcement));
        }

        @Test
        void deleteAnnouncement_Success_TeacherDeletesAnnouncement() {
            // Given: Giáo viên đăng nhập và muốn xóa một thông báo của sinh viên
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockTeacherAccess(TEACHER_ID, true);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, STUDENT_ID, AnnouncementType.GENERIC);

            // When: Thực hiện xóa thông báo
            service.deleteAnnouncement(ANNOUNCEMENT_ID);

            // Then: Thông báo phải được đánh dấu xóa thành công
            assertTrue(announcement.getIsDeleted());
            verify(announcementRepository).save(eq(announcement));
        }

        @Test
        void deleteAnnouncement_Fail_ThrowsWhenAnnouncementMissing() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            assertThrows(AppException.class, () -> service.deleteAnnouncement(99L));

            verify(announcementRepository, never()).save(any(Announcement.class));
        }

        @Test
        void deleteAnnouncement_Fail_ThrowsWhenStudentDeletesOthersAnnouncement() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockStudentAccess(STUDENT_ID);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            assertThrows(AppException.class, () -> service.deleteAnnouncement(ANNOUNCEMENT_ID));

            assertFalse(announcement.getIsDeleted());
            assertAnnouncementNotSaved(announcement);
        }

        @Test
        void deleteAnnouncement_Fail_ThrowsWhenAssistantDeletesOthersAnnouncement() {
            mockCurrentUser(ASSISTANT_ID, Role.STUDENT);
            mockAssistantAccess(ASSISTANT_ID);

            Announcement announcement = saveAnnouncement(ANNOUNCEMENT_ID, TEACHER_ID, AnnouncementType.GENERIC);

            assertThrows(AppException.class, () -> service.deleteAnnouncement(ANNOUNCEMENT_ID));

            assertFalse(announcement.getIsDeleted());
            assertAnnouncementNotSaved(announcement);
        }
    }

    @Nested
    class NotifyAnnouncementTests {

        @Test
        void notifyAnnouncement_UsesObjectIdWhenObjectIdExists() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            Announcement announcement = Announcement.builder()
                    .announcementId(ANNOUNCEMENT_ID)
                    .classroomId(CLASSROOM_ID)
                    .objectId(100L)
                    .type(AnnouncementType.ASSIGNMENT)
                    .build();

            service.notifyAnnouncement(announcement);

            verify(notificationService).createNotificationForClass(
                    any(User.class),
                    eq(CLASSROOM_ID),
                    eq(NotificationObjectType.ASSIGNMENT),
                    eq(100L));
        }

        @Test
        void notifyAnnouncement_UsesAnnouncementIdWhenObjectIdIsNull() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            Announcement announcement = Announcement.builder()
                    .announcementId(ANNOUNCEMENT_ID)
                    .classroomId(CLASSROOM_ID)
                    .objectId(null)
                    .type(AnnouncementType.EXAM)
                    .build();

            service.notifyAnnouncement(announcement);

            verify(notificationService).createNotificationForClass(
                    any(User.class),
                    eq(CLASSROOM_ID),
                    eq(NotificationObjectType.EXAM_CREATED),
                    eq(ANNOUNCEMENT_ID));
        }
    }
}