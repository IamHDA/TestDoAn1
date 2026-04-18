package com.vn.backend;

import com.vn.backend.dto.websocket.StudentJoinedEvent;
import com.vn.backend.entities.Notification;
import com.vn.backend.entities.User;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.impl.WebSocketServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketServiceImpl Unit Tests")
class WebSocketServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisService redisService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private WebSocketServiceImpl webSocketService;

    @Test
    @DisplayName("sendNotificationToUser - gọi convertAndSendToUser đúng topic")
    void sendNotificationToUser_Success() {
        Notification notification = new Notification();
        notification.setNotificationId(1L);

        webSocketService.sendNotificationToUser(100L, notification);

        verify(messagingTemplate).convertAndSendToUser(
                eq("100"),
                eq("/queue/notifications"),
                any()
        );
    }

    @Test
    @DisplayName("broadcastStudentJoined - gọi convertAndSend đúng topic monitoring")
    void broadcastStudentJoined_Success() {
        User student = User.builder()
                .id(2L)
                .code("ST002")
                .fullName("Student Two")
                .email("student2@test.com")
                .build();
        LocalDateTime now = LocalDateTime.now();

        webSocketService.broadcastStudentJoined(1L, student, now);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/exam/1/monitoring"),
                any(StudentJoinedEvent.class)
        );
    }
}
