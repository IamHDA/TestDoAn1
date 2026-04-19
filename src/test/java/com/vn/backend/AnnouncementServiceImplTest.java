package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.announcement.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AnnouncementServiceImpl Unit Tests")
class AnnouncementServiceImplTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private AuthService authService;
    @Mock private NotificationService notificationService;
    @Mock private ClassroomSettingRepository classroomSettingRepository;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private User teacherUser;
    private User studentUser;
    private Classroom classroom;
    private ClassMember activeMember;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).username("teacher").role(Role.TEACHER).build();
        studentUser = User.builder().id(2L).username("student").role(Role.STUDENT).build();
        
        classroom = Classroom.builder().classroomId(100L).className("Lớp Test").build();
        
        activeMember = ClassMember.builder()
                .memberId(10L).classroomId(100L).userId(2L)
                .memberStatus(ClassMemberStatus.ACTIVE).memberRole(ClassMemberRole.STUDENT).build();
                
        announcement = Announcement.builder()
                .announcementId(500L).classroomId(100L).title("Thông báo 1").content("Nội dung").type(AnnouncementType.GENERIC)
                .createdBy(1L).createdByUser(teacherUser).isDeleted(false).build();
                
        when(messageUtils.getMessage(anyString())).thenReturn("Error message");
        when(authService.getCurrentUser()).thenReturn(teacherUser);
    }

    // ================== CREATE ANNOUNCEMENT ==================
    @Test
    @DisplayName("TC_QLLH_ANN_01: createAnnouncement - ném exp 400 khi classroom không tồn tại")
    void createAnnouncement_ThrowsException_ClassNotFound() {
        when(classroomRepository.findByClassroomIdAndClassroomStatus(999L, ClassroomStatus.ACTIVE)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> announcementService.createAnnouncement(999L, new AnnouncementCreateRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_02: createAnnouncement - ném FORBIDDEN khi user không có quyền vào class")
    void createAnnouncement_ThrowsException_ForbiddenAccess() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        // User is not even an active member
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.createAnnouncement(100L, new AnnouncementCreateRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_03: createAnnouncement - học sinh không được đăng nếu lớp cấm post")
    void createAnnouncement_ThrowsException_StudentCannotPost() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));
        
        // Setting cấm post
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(true);

        assertThatThrownBy(() -> announcementService.createAnnouncement(100L, new AnnouncementCreateRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_04: createAnnouncement - học sinh không được phép tạo MATERIAL (Tài liệu)")
    void createAnnouncement_ThrowsException_StudentCreatesMaterial() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(false);

        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setType(AnnouncementType.MATERIAL);

        assertThatThrownBy(() -> announcementService.createAnnouncement(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_05: createAnnouncement - tạo thành công thông báo kèm files đính kèm")
    void createAnnouncement_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(false);

        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setTitle("Test title");
        request.setType(AnnouncementType.GENERIC);
        
        AnnouncementCreateRequest.AttachmentRequest attReq = new AnnouncementCreateRequest.AttachmentRequest();
        attReq.setFileName("file.pdf");
        request.setAttachments(List.of(attReq));

        when(announcementRepository.saveAndFlush(any(Announcement.class))).thenReturn(announcement);

        announcementService.createAnnouncement(100L, request);

        verify(announcementRepository).saveAndFlush(any(Announcement.class));
        verify(attachmentRepository).saveAll(anyList());
        verify(notificationService).createNotificationForClass(eq(teacherUser), eq(100L), eq(NotificationObjectType.ANNOUNCEMENT), eq(500L));
    }

    // ================== GET LIST ==================
    @Test
    @DisplayName("TC_QLLH_ANN_06: getAnnouncementList - ném FORBIDDEN khi user ngoài lớp")
    void getAnnouncementList_ThrowsForbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        BaseFilterSearchRequest<AnnouncementFilterRequest> req = new BaseFilterSearchRequest<>();
        
        assertThatThrownBy(() -> announcementService.getAnnouncementList(100L, new AnnouncementListRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_07: getAnnouncementList - lấy list thành công & check role")
    void getAnnouncementList_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        
        AnnouncementListRequest req = new AnnouncementListRequest();
        req.setFilters(new AnnouncementFilterRequest());
        req.setPagination(new SearchRequest());
        
        Page<Announcement> page = new PageImpl<>(List.of(announcement));
        when(announcementRepository.findByClassroomIdWithFilters(eq(100L), any(), any(Pageable.class))).thenReturn(page);
        
        var result = announcementService.getAnnouncementList(100L, req);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().iterator().next().getCanEdit()).isTrue(); // Teacher có quyền
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }

    // ================== GET DETAIL ==================
    @Test
    @DisplayName("TC_QLLH_ANN_08: getAnnouncementDetail - ném 400 khi ID không tồn tại")
    void getAnnouncementDetail_NotFound() {
        when(announcementRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getAnnouncementDetail(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_09: getAnnouncementDetail - parse detail thành công cùng attachments")
    void getAnnouncementDetail_Success() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));
        
        Attachment att = Attachment.builder().attachmentId(10L).isDeleted(false).fileName("Test.pdf").build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(eq(500L), any())).thenReturn(List.of(att));

        var result = announcementService.getAnnouncementDetail(500L);
        
        assertThat(result.getAnnouncementId()).isEqualTo(500L);
        assertThat(result.getCanEdit()).isFalse(); // student không tự tạo bài này (do teacher tạo), nên canEdit = false
        assertThat(result.getAttachments()).hasSize(1);
        assertThat(result.getAttachments().iterator().next().getFileName()).isEqualTo("Test.pdf");
    }

    // ================== UPDATE ANNOUNCEMENT ==================
    @Test
    @DisplayName("TC_QLLH_ANN_10: updateAnnouncement - Không tồn tại")
    void updateAnnouncement_NotFound() {
        when(announcementRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> announcementService.updateAnnouncement(999L, new AnnouncementUpdateRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_ANN_11: updateAnnouncement - Cấm Student sửa bài của người khác")
    void updateAnnouncement_ForbiddenStudent() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement)); // Do teacherUser (ID=1) tạo
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));

        assertThatThrownBy(() -> announcementService.updateAnnouncement(500L, new AnnouncementUpdateRequest()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_12: updateAnnouncement - Student tự tạo có thể chỉnh sửa")
    void updateAnnouncement_SuccessStudentCreator() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // ID = 2
        Announcement studentAnn = Announcement.builder().announcementId(501L).classroomId(100L).createdBy(2L).build();
        when(announcementRepository.findByIdAndNotDeleted(501L)).thenReturn(Optional.of(studentAnn)); 
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest();
        req.setContent("Content changed");
        announcementService.updateAnnouncement(501L, req);

        verify(announcementRepository).save(studentAnn);
        assertThat(studentAnn.getContent()).isEqualTo("Content changed");
    }

    @Test
    @DisplayName("TC_QLLH_ANN_13: updateAnnouncement - Teacher sửa bài thành công mà không cần kiểm tra quyền createdBy")
    void updateAnnouncement_SuccessTeacher() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID = 1
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true); // Is Teacher!

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest();
        announcementService.updateAnnouncement(500L, req);

        verify(announcementRepository).save(announcement);
    }

    @Test
    @DisplayName("TC_QLLH_ANN_14: updateAnnouncement - Chỉnh sửa kèm xử lý file attachments (Xóa cũ, Thêm mới)")
    void updateAnnouncement_AttachmentsLogic() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Attachment oldAtt = Attachment.builder().attachmentId(10L).isDeleted(false).build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(eq(500L), any())).thenReturn(List.of(oldAtt));

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest();
        AnnouncementUpdateRequest.AttachmentUpdateRequest attReq1 = new AnnouncementUpdateRequest.AttachmentUpdateRequest();
        attReq1.setAttachmentId(10L); // Giữ lại cũ
        AnnouncementUpdateRequest.AttachmentUpdateRequest attReq2 = new AnnouncementUpdateRequest.AttachmentUpdateRequest();
        attReq2.setAttachmentId(null); // Thêm file mới
        
        req.setAttachments(List.of(attReq1, attReq2));

        announcementService.updateAnnouncement(500L, req);

        // Đảm bảo file mới sẽ đc lưu
        verify(attachmentRepository).save(any(Attachment.class));
        // Id = 10L giữ nguyên do có truyền trong Request, không xóa
        verify(attachmentRepository, never()).softDeleteById(10L);
    }
    
    @Test
    @DisplayName("TC_QLLH_ANN_14b: updateAnnouncement - Nếu request rỗng thì xóa toàn bộ Attachment hiện tại")
    void updateAnnouncement_EmptyAttachments() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        Attachment oldAtt = Attachment.builder().attachmentId(10L).isDeleted(false).build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(eq(500L), any())).thenReturn(List.of(oldAtt));

        AnnouncementUpdateRequest req = new AnnouncementUpdateRequest();
        req.setAttachments(new ArrayList<>()); // danh sách rỗng 

        announcementService.updateAnnouncement(500L, req);

        // Đảm bảo file cũ bị xóa
        verify(attachmentRepository).softDeleteById(10L);
    }

    // ================== DELETE ANNOUNCEMENT ==================
    @Test
    @DisplayName("TC_QLLH_ANN_15: deleteAnnouncement - Ném exp khi bài viết ko tồn tại")
    void deleteAnnouncement_NotFound() {
        when(announcementRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> announcementService.deleteAnnouncement(999L)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_ANN_16: deleteAnnouncement - Ném exp FORBIDDEN khi k đủ quyền")
    void deleteAnnouncement_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(activeMember));

        assertThatThrownBy(() -> announcementService.deleteAnnouncement(500L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_ANN_17: deleteAnnouncement - Xóa mềm (Soft delete) thành công")
    void deleteAnnouncement_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(announcementRepository.findByIdAndNotDeleted(500L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        announcementService.deleteAnnouncement(500L);

        verify(announcementRepository).save(announcement);
        assertThat(announcement.getIsDeleted()).isTrue();
    }
}
