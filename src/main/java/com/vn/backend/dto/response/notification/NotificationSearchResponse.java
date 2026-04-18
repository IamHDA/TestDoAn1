package com.vn.backend.dto.response.notification;

import com.vn.backend.utils.ModelMapperUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationSearchResponse {
    private Long notificationId;
    private String objectType;
    private String objectId;
    private String priority;
    private Long classroomId;
    private String title;
    private boolean isRead;
    private LocalDateTime deliveryAt;

    public static NotificationSearchResponse fromDTO(NotificationSearchQueryDTO dto) {
        return ModelMapperUtils.mapTo(dto, NotificationSearchResponse.class);
    }
}
