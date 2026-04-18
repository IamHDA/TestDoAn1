package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.notification.NotificationSearchQueryDTO;
import com.vn.backend.dto.response.notification.NotificationSearchResponse;
import com.vn.backend.entities.*;
import com.vn.backend.enums.NotificationDeliveryStatus;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.NotificationPriority;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.NotificationRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.utils.MessageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.vn.backend.constants.AppConst.TITLE_NOTIFICATION_MAP;

@Service
public class NotificationServiceImpl extends BaseService implements NotificationService {

    private final AuthService authService;
    private final NotificationRepository notificationRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final WebSocketService webSocketService;

    public NotificationServiceImpl(MessageUtils messageUtils,
                                   AuthService authService,
                                   NotificationRepository notificationRepository,
                                   ClassMemberRepository classMemberRepository,
                                   ClassroomRepository classroomRepository, WebSocketService webSocketService) {
        super(messageUtils);
        this.authService = authService;
        this.notificationRepository = notificationRepository;
        this.classMemberRepository = classMemberRepository;
        this.classroomRepository = classroomRepository;
        this.webSocketService = webSocketService;
    }


    @Override
    public long countUnreadNotification() {
        log.info("Start service to count unread notification");

        User user = authService.getCurrentUser();
        long unreadNotification = notificationRepository.countUnreadNotification(user.getId());

        log.info("End service count unread notification");
        return unreadNotification;
    }

    @Override
    public ResponseListData<NotificationSearchResponse> searchNotification(BaseFilterSearchRequest<Void> request) {
        log.info("Start service to search notification");

        User user = authService.getCurrentUser();
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<NotificationSearchQueryDTO> queryDTOS = notificationRepository.searchNotification(user.getId(), NotificationDeliveryStatus.SENT, pageable);

        List<NotificationSearchResponse> responseList = queryDTOS.stream()
                .map(NotificationSearchResponse::fromDTO)
                .toList();
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());

        log.info("End service search notification");
        return new ResponseListData<>(responseList, pagingMeta);
    }

    @Override
    public void updateIsReadNotification(String notificationId) {
        log.info("Start service to count unread notification");

        User user = authService.getCurrentUser();
        Notification notification = notificationRepository.findByNotificationIdAndReceiverId(
                        Long.parseLong(notificationId), user.getId())
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("End service count unread notification");
    }

    @Override
    public void readAllNotification() {
        log.info("Start service to read all notification");

        User user = authService.getCurrentUser();
        List<Notification> notifications = notificationRepository.findAllByIsReadFalseAndReceiverId(user.getId());
        notifications.forEach(e -> {
            e.setRead(true);
            e.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(notifications);

        log.info("End service read all notification");
    }

    @Override
    public void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            Invitation invitation) {
        if (sender == null || receiverId == null || notificationObjectType == null || objectId == null) {
            log.info("Create notification for user failed");
            return;
        }
        String senderName = sender.getFullName();
        String template = TITLE_NOTIFICATION_MAP.get(notificationObjectType);
        String title = String.format(template, senderName, invitation.getClassroom().getClassName());
        Notification notification = this.initAndSaveNotification(sender, receiverId, notificationObjectType, objectId, title, invitation.getClassroomId());

        webSocketService.sendNotificationToUser(receiverId, notification);
    }

    @Override
    public void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            Classroom classroom) {
        if (sender == null || receiverId == null || notificationObjectType == null || objectId == null) {
            log.info("Create notification for user failed");
            return;
        }
        String senderName = sender.getFullName();
        String template = TITLE_NOTIFICATION_MAP.get(notificationObjectType);
        String title = String.format(template, senderName, classroom.getClassName());
        Notification notification = this.initAndSaveNotification(sender, receiverId, notificationObjectType, objectId, title, classroom.getClassroomId());

        webSocketService.sendNotificationToUser(receiverId, notification);
    }

    @Override
    public void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            SessionExam sessionExam) {
        if (sender == null || receiverId == null || notificationObjectType == null || objectId == null) {
            log.info("Create notification for user failed");
            return;
        }
        String senderName = sender.getFullName();
        String template = TITLE_NOTIFICATION_MAP.get(notificationObjectType);
        String title = String.format(template, senderName, sessionExam.getTitle());
        Notification notification = this.initAndSaveNotification(sender, receiverId, notificationObjectType, objectId, title, sessionExam.getClassId());

        webSocketService.sendNotificationToUser(receiverId, notification);
    }

    @Override
    public void createNotificationForClass(
            User sender,
            Long classroomId,
            NotificationObjectType notificationObjectType,
            Long objectId) {

        if (sender == null || classroomId == null || notificationObjectType == null || objectId == null) {
            log.info("Create notification for class failed");
            return;
        }

        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        Set<Long> memberIds = classMemberRepository.getClassMemberIdsActive(classroomId, null);
        memberIds.add(classroom.getTeacherId());
        memberIds.remove(sender.getId());

        String senderName = sender.getFullName();
        String template = TITLE_NOTIFICATION_MAP.get(notificationObjectType);
        String title = String.format(template, senderName, classroom.getClassName());

        for (Long memberId : memberIds) {
            Notification notification = this.initAndSaveNotification(sender, memberId, notificationObjectType, objectId, title, classroomId);

            // Gửi thông báo real-time qua WebSocket
            webSocketService.sendNotificationToUser(memberId, notification);
        }
    }

    private Notification initAndSaveNotification(User sender, Long receiverId, NotificationObjectType notificationObjectType, Long objectId, String title, Long classroomId) {
        Notification notification = new Notification();
        notification.setSenderId(sender.getId());
        notification.setReceiverId(receiverId);
        notification.setClassroomId(classroomId);
        notification.setObjectId(objectId);
        notification.setObjectType(notificationObjectType);
        notification.setPriority(this.getPriorityByNotificationType(notificationObjectType));
        notification.setTitle(title);
        notification.setDeliveryAt(LocalDateTime.now()); // TODO: tạm thời
        notification.setDeliveryStatus(NotificationDeliveryStatus.SENT); // TODO: tạm thời
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    private NotificationPriority getPriorityByNotificationType(NotificationObjectType type) {
        return switch (type) {
            case ASSIGNMENT, EXAM_CREATED -> NotificationPriority.HIGH;
            case INVITE_CLASS, JOIN_CLASS, ANNOUNCEMENT, MATERIAL -> NotificationPriority.MEDIUM;
            default -> NotificationPriority.LOW;
        };
    }
}
