package com.vn.backend.dto.response.notification;

import com.vn.backend.entities.Notification;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.NotificationPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationWebSocketResponse {

    private Long notificationId;
    private NotificationObjectType objectType;
    private Long objectId;
    private Long classroomId;
    private NotificationPriority priority;
    private String title;
    private boolean isRead;
    private LocalDateTime deliveryAt;

    public static NotificationWebSocketResponse fromEntity(Notification notification) {
        return NotificationWebSocketResponse.builder()
                .notificationId(notification.getNotificationId())
                .objectType(notification.getObjectType())
                .objectId(notification.getObjectId())
                .classroomId(notification.getClassroomId())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .isRead(notification.isRead())
                .deliveryAt(notification.getDeliveryAt())
                .build();
    }
}