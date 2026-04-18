package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.notification.NotificationSearchResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.Invitation;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.NotificationObjectType;

import static com.vn.backend.constants.AppConst.TITLE_NOTIFICATION_MAP;

public interface NotificationService {

    long countUnreadNotification();

    ResponseListData<NotificationSearchResponse> searchNotification(BaseFilterSearchRequest<Void> request);

    void updateIsReadNotification(String notificationId);

    void readAllNotification();

    void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            Invitation invitation);

    void createNotificationForClass(
            User sender,
            Long classroomId,
            NotificationObjectType notificationObjectType,
            Long objectId);

    void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            Classroom classroom);

    void createNotificationForUser(
            User sender,
            Long receiverId,
            NotificationObjectType notificationObjectType,
            Long objectId,
            SessionExam sessionExam);
}
