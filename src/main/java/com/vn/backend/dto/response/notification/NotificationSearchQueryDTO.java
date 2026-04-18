package com.vn.backend.dto.response.notification;

import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.NotificationPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationSearchQueryDTO {
    private Long notificationId;
    private NotificationObjectType objectType;
    private Long objectId;
    private Long classroomId;
    private NotificationPriority priority;
    private String title;
    private boolean isRead;
    private LocalDateTime deliveryAt;
}
