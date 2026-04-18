package com.vn.backend.repositories;

import com.vn.backend.dto.response.notification.NotificationSearchQueryDTO;
import com.vn.backend.entities.Notification;
import com.vn.backend.enums.NotificationDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT count(noti)
            from Notification noti
            where noti.receiverId = :userId
            and noti.isRead = false
            """)
    long countUnreadNotification(Long userId);

    Optional<Notification> findByNotificationIdAndReceiverId(Long notificationId, Long receiverId);

    List<Notification> findAllByIsReadFalseAndReceiverId(Long receiverId);
    @Query("""
            select new com.vn.backend.dto.response.notification.NotificationSearchQueryDTO(
                n.notificationId,
                n.objectType,
                n.objectId,
                n.classroomId,
                n.priority,
                n.title,
                n.isRead,
                n.deliveryAt
            )
            from Notification n
            where n.receiverId = :receiverId
            and n.deliveryStatus = :deliveryStatus
            """)
    Page<NotificationSearchQueryDTO> searchNotification(Long receiverId, NotificationDeliveryStatus deliveryStatus, Pageable pageable);
}
