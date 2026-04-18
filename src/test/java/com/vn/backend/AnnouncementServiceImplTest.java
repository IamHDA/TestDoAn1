package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.Attachment;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.User;
import com.vn.backend.enums.AnnouncementType;
import com.vn.backend.enums.AttachmentType;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnnouncementRepository;
import com.vn.backend.repositories.AttachmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.ClassroomSettingRepository;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementServiceImpl Unit Tests")
class AnnouncementServiceImplTest {

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

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private User teacherUser;
    private User studentUser;
    private Classroom classroom;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .username("teacher1")
                .fullName("Teacher Name")
                .role(Role.TEACHER)
                .build();

        studentUser = User.builder()
                .id(2L)
                .username("student1")
                .fullName("Student Name")
                .role(Role.STUDENT)
                .build();

        classroom = Classroom.builder()
                .classroomId(100L)
                .className("Math 101")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .teacherId(1L)
                .build();

        announcement = Announcement.builder()
                .announcementId(10L)
                .classroomId(100L)
                .title("Welcome")
                .content("Welcome to the class!")
                .type(AnnouncementType.GENERIC)
                .createdBy(1L)
                .createdByUser(teacherUser)
                .isDeleted(false)
                .build();
    }

    // ===================== createAnnouncement =====================

    @Test
    @DisplayName("createAnnouncement - thành công khi giáo viên đăng")
    void createAnnouncement_Success_WhenTeacher() {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setTitle("Hello");
        request.setContent("Hi everyone");
        request.setType(AnnouncementType.GENERIC);
        request.setAllowComments(true);

        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true); 
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(false);
        
        Announcement savedAnnouncement = Announcement.builder().announcementId(50L).classroomId(100L).type(AnnouncementType.GENERIC).build();
        when(announcementRepository.saveAndFlush(any(Announcement.class))).thenReturn(savedAnnouncement);

        announcementService.createAnnouncement(100L, request);

        verify(announcementRepository).saveAndFlush(any(Announcement.class));
        verify(notificationService).createNotificationForClass(
                eq(teacherUser), eq(100L), eq(NotificationObjectType.ANNOUNCEMENT), eq(50L)
        );
    }

    @Test
    @DisplayName("createAnnouncement - ném exception khi học sinh đăng nhưng setting bị tắt")
    void createAnnouncement_ThrowsException_WhenStudentPostDisabled() {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setType(AnnouncementType.GENERIC);

        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        
        ClassMember studentMember = ClassMember.builder().memberStatus(ClassMemberStatus.ACTIVE).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));
        
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(true); // Học sinh bị cấm đăng bài
        when(messageUtils.getMessage(AppConst.MessageConst.CANNOT_POST)).thenReturn("Cannot post");

        assertThatThrownBy(() -> announcementService.createAnnouncement(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.CANNOT_POST);
                });
    }

    @Test
    @DisplayName("createAnnouncement - ném exception khi role khác TEACHER mà tạo tài liệu")
    void createAnnouncement_ThrowsException_WhenMaterialButNotTeacher() {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setType(AnnouncementType.MATERIAL);

        when(authService.getCurrentUser()).thenReturn(studentUser); // role STUDENT
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));
        
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        
        ClassMember studentMember = ClassMember.builder().memberStatus(ClassMemberStatus.ACTIVE).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));
        when(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(100L)).thenReturn(false);

        when(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN)).thenReturn("Forbidden");

        assertThatThrownBy(() -> announcementService.createAnnouncement(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FORBIDDEN);
                });
    }

    // ===================== getAnnouncementDetail =====================

    @Test
    @DisplayName("getAnnouncementDetail - thành công lấy thông tin chi tiết")
    void getAnnouncementDetail_Success() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // student
        when(announcementRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(announcement));

        // Mock permission logic
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        ClassMember studentMember = ClassMember.builder().memberStatus(ClassMemberStatus.ACTIVE).memberRole(ClassMemberRole.STUDENT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));
        
        // Mock attachments
        Attachment att = Attachment.builder()
                .attachmentId(1000L).fileName("doc.pdf").fileUrl("http://url").isDeleted(false).build();
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(10L, AttachmentType.GENERIC))
                .thenReturn(List.of(att));

        AnnouncementResponse response = announcementService.getAnnouncementDetail(10L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Welcome");
        assertThat(response.getAttachments()).hasSize(1);
        assertThat(response.getAttachments().get(0).getFileName()).isEqualTo("doc.pdf");
        assertThat(response.getCanEdit()).isFalse(); // student không phải người tạo đăng
    }

    // ===================== updateAnnouncement =====================

    @Test
    @DisplayName("updateAnnouncement - thành công khi teacher cập nhật")
    void updateAnnouncement_Success_WhenTeacher() {
        AnnouncementUpdateRequest request = new AnnouncementUpdateRequest();
        request.setTitle("New Title");
        request.setContent("New Content");

        when(authService.getCurrentUser()).thenReturn(teacherUser); // ID = 1
        when(announcementRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true); // Is Teacher

        announcementService.updateAnnouncement(10L, request);

        assertThat(announcement.getTitle()).isEqualTo("New Title");
        verify(announcementRepository).save(announcement);
    }

    // ===================== deleteAnnouncement =====================

    @Test
    @DisplayName("deleteAnnouncement - ném lỗi Forbidden khi học sinh không phải chủ xóa")
    void deleteAnnouncement_ThrowsException_WhenStudentNotOwner() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // ID = 2
        when(announcementRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(announcement)); // posted by ID = 1
        
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        ClassMember studentMember = ClassMember.builder().memberStatus(ClassMemberStatus.ACTIVE).memberRole(ClassMemberRole.STUDENT).build();
        when(classMemberRepository.findByClassroomIdAndUserId(100L, 2L)).thenReturn(Optional.of(studentMember));
        
        when(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN)).thenReturn("Forbidden");
        
        assertThatThrownBy(() -> announcementService.deleteAnnouncement(10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("deleteAnnouncement - thành công khi soft delete")
    void deleteAnnouncement_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // Teacher is the owner & the teacher of class
        when(announcementRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(announcement));
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        announcementService.deleteAnnouncement(10L);

        assertThat(announcement.getIsDeleted()).isTrue();
        verify(announcementRepository).save(announcement);
    }
}
