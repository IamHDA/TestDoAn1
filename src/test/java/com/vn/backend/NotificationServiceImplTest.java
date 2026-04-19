package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.notification.NotificationSearchQueryDTO;
import com.vn.backend.dto.response.notification.NotificationSearchResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.Invitation;
import com.vn.backend.entities.Notification;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.NotificationDeliveryStatus;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.NotificationRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.services.impl.NotificationServiceImpl;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User currentUser;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .fullName("Test User")
                .build();

        classroom = Classroom.builder()
                .classroomId(100L)
                .className("Math 101")
                .teacherId(2L)
                .build();
    }

    // ===================== countUnreadNotification =====================

    @Test
    @DisplayName("countUnreadNotification - trả về số lượng chính xác")
    void countUnreadNotification_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(notificationRepository.countUnreadNotification(1L)).thenReturn(5L);

        long count = notificationService.countUnreadNotification();

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countUnreadNotification(1L);
    }

    // ===================== searchNotification =====================

    @Test
    @DisplayName("searchNotification - trả về danh sách phân trang thành công")
    void searchNotification_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);

        BaseFilterSearchRequest<Void> request = mock(BaseFilterSearchRequest.class);
        SearchRequest pagination = mock(SearchRequest.class);
        PagingMeta pagingMeta = new PagingMeta();
        pagingMeta.setPageNum(1);
        pagingMeta.setPageSize(10);
        
        when(request.getPagination()).thenReturn(pagination);
        when(pagination.getPagingMeta()).thenReturn(pagingMeta);

        NotificationSearchQueryDTO dto = NotificationSearchQueryDTO.builder()
                .notificationId(10L)
                .title("Title")
                .objectType(NotificationObjectType.ASSIGNMENT)
                .objectId(1L)
                .classroomId(100L)
                .isRead(false)
                .deliveryAt(LocalDateTime.now())
                .build();

        Page<NotificationSearchQueryDTO> page = new PageImpl<>(List.of(dto));

        when(notificationRepository.searchNotification(
                eq(1L),
                eq(NotificationDeliveryStatus.SENT),
                any(Pageable.class)
        )).thenReturn(page);

        ResponseListData<NotificationSearchResponse> response = notificationService.searchNotification(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPaging().getTotalRows()).isEqualTo(1L);
    }

    // ===================== updateIsReadNotification =====================

    @Test
    @DisplayName("updateIsReadNotification - đánh dấu đã đọc thành công")
    void updateIsReadNotification_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);

        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setRead(false);

        when(notificationRepository.findByNotificationIdAndReceiverId(10L, 1L))
                .thenReturn(Optional.of(notification));

        notificationService.updateIsReadNotification("10");

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("updateIsReadNotification - ném exception khi notification không tồn tại")
    void updateIsReadNotification_ThrowsException() {
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(notificationRepository.findByNotificationIdAndReceiverId(10L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> notificationService.updateIsReadNotification("10"))
                .isInstanceOf(AppException.class);
    }

    // ===================== readAllNotification =====================

    @Test
    @DisplayName("readAllNotification - đánh dấu đọc toàn bộ")
    void readAllNotification_Success() {
        when(authService.getCurrentUser()).thenReturn(currentUser);

        Notification n1 = new Notification();
        n1.setRead(false);
        Notification n2 = new Notification();
        n2.setRead(false);

        when(notificationRepository.findAllByIsReadFalseAndReceiverId(1L))
                .thenReturn(List.of(n1, n2));

        notificationService.readAllNotification();

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    // ===================== createNotificationForUser (Invitation) =====================

    @Test
    @DisplayName("createNotificationForUser(Invitation) - gửi websocket thành công")
    void createNotificationForUser_Invitation_Success() {
        Invitation invitation = Invitation.builder()
                .classroom(classroom)
                .classroomId(100L)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.createNotificationForUser(
                currentUser, 2L, NotificationObjectType.INVITE_CLASS, 1L, invitation
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(2L), any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationForUser(Invitation) - return sớm nếy input null")
    void createNotificationForUser_Invitation_Fail_NullInputs() {
        notificationService.createNotificationForUser(null, 2L, NotificationObjectType.INVITE_CLASS, 1L, new Invitation());
        verify(notificationRepository, never()).save(any());
    }

    // ===================== createNotificationForUser (Classroom) =====================

    @Test
    @DisplayName("createNotificationForUser(Classroom) - gửi thành công")
    void createNotificationForUser_Classroom_Success() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        notificationService.createNotificationForUser(
                currentUser, 2L, NotificationObjectType.JOIN_CLASS, 1L, classroom
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(2L), any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationForUser(Classroom) - return sớm nếu input null")
    void createNotificationForUser_Classroom_Fail_NullInputs() {
        notificationService.createNotificationForUser(currentUser, null, NotificationObjectType.JOIN_CLASS, 1L, classroom);
        verify(notificationRepository, never()).save(any());
    }

    // ===================== createNotificationForUser (SessionExam) =====================

    @Test
    @DisplayName("createNotificationForUser(SessionExam) - gửi thành công")
    void createNotificationForUser_SessionExam_Success() {
        SessionExam sessionExam = SessionExam.builder()
                .title("Midterm Exam")
                .classId(100L)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        notificationService.createNotificationForUser(
                currentUser, 2L, NotificationObjectType.EXAM_CREATED, 1L, sessionExam
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(2L), any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationForUser(SessionExam) - return sớm nếu input null")
    void createNotificationForUser_SessionExam_Fail_NullInputs() {
        notificationService.createNotificationForUser(currentUser, 2L, null, 1L, new SessionExam());
        verify(notificationRepository, never()).save(any());
    }

    // ===================== createNotificationForClass =====================

    @Test
    @DisplayName("createNotificationForClass - gửi đến tất cả thành viên (trừ sender)")
    void createNotificationForClass_Success() {
        when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));

        // Member 3 and 4
        Set<Long> memberIds = new HashSet<>();
        memberIds.add(3L);
        memberIds.add(4L);
        
        // user 1 (sender) id should be removed if it existed, and teacher (2L) should be added.
        when(classMemberRepository.getClassMemberIdsActive(100L, null)).thenReturn(memberIds);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.createNotificationForClass(currentUser, 100L, NotificationObjectType.ANNOUNCEMENT, 50L);

        // Expect notifications sent to: 3, 4, 2 (teacher). user 1 is excluded.
        verify(notificationRepository, times(3)).save(any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(2L), any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(3L), any(Notification.class));
        verify(webSocketService).sendNotificationToUser(eq(4L), any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationForClass - return sớm nếu input null")
    void createNotificationForClass_Fail_NullInputs() {
        notificationService.createNotificationForClass(null, 100L, NotificationObjectType.ANNOUNCEMENT, 1L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createNotificationForClass - ném exception khi không thấy lớp")
    void createNotificationForClass_ThrowsException_WhenNotFound() {
        when(classroomRepository.findById(100L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> notificationService.createNotificationForClass(currentUser, 100L, NotificationObjectType.ANNOUNCEMENT, 1L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Priority Logic - bao phủ nhánh HIGH, MEDIUM, LOW")
    void test_PriorityLogic() throws Exception {
        // Sử dụng reflection để gọi private method getPriorityByNotificationType
        java.lang.reflect.Method method = NotificationServiceImpl.class.getDeclaredMethod("getPriorityByNotificationType", NotificationObjectType.class);
        method.setAccessible(true);

        // HIGH
        assertThat(method.invoke(notificationService, NotificationObjectType.ASSIGNMENT)).isEqualTo(com.vn.backend.enums.NotificationPriority.HIGH);
        assertThat(method.invoke(notificationService, NotificationObjectType.EXAM_CREATED)).isEqualTo(com.vn.backend.enums.NotificationPriority.HIGH);
        
        // MEDIUM
        assertThat(method.invoke(notificationService, NotificationObjectType.INVITE_CLASS)).isEqualTo(com.vn.backend.enums.NotificationPriority.MEDIUM);
        assertThat(method.invoke(notificationService, NotificationObjectType.JOIN_CLASS)).isEqualTo(com.vn.backend.enums.NotificationPriority.MEDIUM);
        assertThat(method.invoke(notificationService, NotificationObjectType.ANNOUNCEMENT)).isEqualTo(com.vn.backend.enums.NotificationPriority.MEDIUM);
        assertThat(method.invoke(notificationService, NotificationObjectType.MATERIAL)).isEqualTo(com.vn.backend.enums.NotificationPriority.MEDIUM);
        
        // LOW (default)
        assertThat(method.invoke(notificationService, NotificationObjectType.SYSTEM_NOTIFICATION)).isEqualTo(com.vn.backend.enums.NotificationPriority.LOW);
    }
}