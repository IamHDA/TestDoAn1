package com.vn.backend.entities;

import com.vn.backend.enums.NotificationDeliveryStatus;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.enums.NotificationPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "notifications")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", length = 20)
    private NotificationObjectType objectType;

    @Column(name = "object_id")
    private Long objectId;

    @Column(name = "classroom_id")
    private Long classroomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private NotificationPriority priority;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 10)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(name = "delivery_at")
    private LocalDateTime deliveryAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;
}
