package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import com.vn.backend.dto.redis.StudentInfoDTO;
import com.vn.backend.dto.websocket.*;
import com.vn.backend.entities.Notification;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ViolationType;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.impl.WebSocketServiceImpl;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketServiceImpl Unit Tests - 100% Coverage Target")
class WebSocketServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisService redisService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private WebSocketServiceImpl webSocketService;

    private User student;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(2L)
                .code("ST002")
                .fullName("Student Two")
                .email("student2@test.com")
                .build();
        now = LocalDateTime.now();
    }

    @Test
    @DisplayName("sendNotificationToUser - Success Path")
    void sendNotificationToUser_Success() {
        Notification notification = new Notification();
        notification.setNotificationId(1L);

        webSocketService.sendNotificationToUser(100L, notification);

        verify(messagingTemplate).convertAndSendToUser(
                eq("100"),
                eq("/queue/notifications"),
                any());
    }

    @Test
    @DisplayName("sendNotificationToUser - Catch Exception Branch")
    void sendNotificationToUser_Exception() {
        Notification notification = new Notification();
        doThrow(new RuntimeException("Simulated error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        webSocketService.sendNotificationToUser(100L, notification);
        // Error is logged and caught
    }

    @Test
    @DisplayName("broadcastStudentJoined - Success Path")
    void broadcastStudentJoined_Success() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            SessionExamStateDTO state = SessionExamStateDTO.builder()
                    .countdownStartAt(now)
                    .duration(60L)
                    .build();
            when(redisService.getExamState("stateKey")).thenReturn(state);

            webSocketService.broadcastStudentJoined(1L, student, now);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
            verify(redisService).lPushLeft(anyString(), anyString());
            verify(redisService).expire(anyString(), anyLong(), any());
        }
    }

    @Test
    @DisplayName("broadcastStudentJoined - Catch Exception Path")
    void broadcastStudentJoined_Exception() {
        doThrow(new RuntimeException("Simulated Error")).when(messagingTemplate)
                .convertAndSend(anyString(), (Object) any());

        webSocketService.broadcastStudentJoined(1L, student, now);
        // Exception caught
    }

    @Test
    @DisplayName("broadcastStudentDownloaded - Success Path")
    void broadcastStudentDownloaded_Success() {
        StudentInfoDTO studentInfo = StudentInfoDTO.builder()
                .studentId(student.getId())
                .studentCode(student.getCode())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .build();

        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            SessionExamStateDTO state = SessionExamStateDTO.builder()
                    .countdownStartAt(now)
                    .duration(60L)
                    .build();
            when(redisService.getExamState("stateKey")).thenReturn(state);

            webSocketService.broadcastStudentDownloaded(1L, studentInfo, now);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
        }
    }

    @Test
    @DisplayName("broadcastStudentDownloaded - Catch Exception")
    void broadcastStudentDownloaded_Exception() {
        StudentInfoDTO studentInfo = StudentInfoDTO.builder().studentId(1L).build();
        doThrow(new RuntimeException("WSError")).when(messagingTemplate).convertAndSend(anyString(), (Object) any());

        webSocketService.broadcastStudentDownloaded(1L, studentInfo, now);
    }

    @Test
    @DisplayName("broadcastStudentSubmitted - Success Path")
    void broadcastStudentSubmitted_Success() {
        StudentInfoDTO studentInfo = StudentInfoDTO.builder().studentId(2L).build();
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            SessionExamStateDTO state = SessionExamStateDTO.builder()
                    .countdownStartAt(now)
                    .duration(60L)
                    .build();
            when(redisService.getExamState("stateKey")).thenReturn(state);

            webSocketService.broadcastStudentSubmitted(1L, studentInfo, 8.5, now);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
        }
    }

    @Test
    @DisplayName("broadcastStudentSubmitted - Catch Exception")
    void broadcastStudentSubmitted_Exception() {
        StudentInfoDTO studentInfo = StudentInfoDTO.builder().studentId(1L).build();
        doThrow(new RuntimeException("Error")).when(messagingTemplate).convertAndSend(anyString(), (Object) any());
        webSocketService.broadcastStudentSubmitted(1L, studentInfo, 5.0, now);
    }

    @Test
    @DisplayName("broadcastGradingCompleted - Success and Exception")
    void broadcastGradingCompleted_AllPaths() {
        // Success
        webSocketService.broadcastGradingCompleted(1L, 50, now);
        verify(messagingTemplate).convertAndSend(eq("/topic/exam/1"), (Object) any());

        // Exception
        doThrow(new RuntimeException()).when(messagingTemplate).convertAndSend(anyString(), (Object) any());
        webSocketService.broadcastGradingCompleted(1L, 50, now);
    }

    @Test
    @DisplayName("handleHeartbeat - Success Path")
    void handleHeartbeat_Success() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setStudentId(2L);

        Map<String, Object> statusData = new HashMap<>();
        statusData.put(AppConst.FieldConst.STUDENT_CODE, "ST002");
        statusData.put(AppConst.FieldConst.FULL_NAME, "Student Two");

        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.sessionActiveStudents(anyLong(), anyLong())).thenReturn("activeKey");
            redisUtils.when(() -> RedisUtils.studentStatus(anyLong(), anyLong())).thenReturn("statusKey");

            when(redisService.hGetAllAsString("statusKey")).thenReturn(statusData);

            webSocketService.handleHeartbeat(1L, request);

            verify(redisService).set(eq("activeKey"), eq("2"), anyLong(), eq(TimeUnit.SECONDS));
            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
        }
    }

    @Test
    @DisplayName("handleHeartbeat - Catch Exception")
    void handleHeartbeat_Exception() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setStudentId(2L);
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.sessionActiveStudents(anyLong(), anyLong()))
                    .thenThrow(new RuntimeException("Redis error"));
            webSocketService.handleHeartbeat(1L, request);
        }
    }

    @Test
    @DisplayName("handleViolation - Success Path")
    void handleViolation_Success() {
        ViolationRequest request = new ViolationRequest();
        request.setStudentId(2L);
        request.setViolationType(ViolationType.TAB_SWITCH);
        request.setDescription("Switched tab");

        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .countdownStartAt(now)
                .duration(30L)
                .build();

        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.studentStatus(anyLong(), anyLong())).thenReturn("statusKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            when(redisService.getExamState("stateKey")).thenReturn(state);
            when(redisService.hIncrement(anyString(), anyString(), anyLong())).thenReturn(1L);
            when(redisService.hGet(anyString(), anyString())).thenReturn("some-info");
            when(redisService.parseString(any())).thenReturn("parsed-info");

            webSocketService.handleViolation(1L, request);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
            verify(redisService).lPushLeft(eq("historyKey"), anyString());
            verify(redisService).expire(eq("historyKey"), anyLong(), any());
        }
    }

    @Test
    @DisplayName("handleViolation - Success Path without Description")
    void handleViolation_Success_NoDescription() {
        ViolationRequest request = new ViolationRequest();
        request.setStudentId(2L);
        request.setViolationType(ViolationType.TAB_SWITCH);
        request.setDescription(null);

        SessionExamStateDTO state = SessionExamStateDTO.builder()
                .countdownStartAt(now)
                .duration(30L)
                .build();

        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.studentStatus(anyLong(), anyLong())).thenReturn("statusKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            when(redisService.getExamState("stateKey")).thenReturn(state);
            when(redisService.hIncrement(anyString(), anyString(), anyLong())).thenReturn(1L);
            when(redisService.hGet(anyString(), anyString())).thenReturn("some-info");
            when(redisService.parseString(any())).thenReturn("parsed-info");

            webSocketService.handleViolation(1L, request);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
        }
    }

    @Test
    @DisplayName("handleViolation - Catch Exception")
    void handleViolation_Exception() {
        ViolationRequest request = new ViolationRequest();
        request.setStudentId(2L);
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong()))
                    .thenThrow(new RuntimeException("Critical Error"));
            webSocketService.handleViolation(1L, request);
        }
    }

    @Test
    @DisplayName("publishOffline - Success Path")
    void publishOffline_Success() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.studentStatus(anyLong(), anyLong())).thenReturn("statusKey");
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            when(redisService.hGet(anyString(), anyString())).thenReturn("value");
            when(redisService.parseString(any())).thenReturn("parsedValue");

            SessionExamStateDTO state = SessionExamStateDTO.builder()
                    .countdownStartAt(now)
                    .duration(10L)
                    .build();
            when(redisService.getExamState("stateKey")).thenReturn(state);

            webSocketService.publishOffline(1L, 2L);

            verify(messagingTemplate).convertAndSend(eq("/topic/exam/1/monitoring"), (Object) any());
            verify(redisService).lPushLeft(eq("historyKey"), anyString());
            verify(redisService).expire(eq("historyKey"), anyLong(), any());
        }
    }

    @Test
    @DisplayName("parseString - Helper Logic")
    void parseString_Logic() {
        assert webSocketService.parseString(null) == null;
        assert webSocketService.parseString("test").equals("test");
        assert webSocketService.parseString(123).equals("123");
    }

    @Test
    @DisplayName("calculateTtl - Logic Coverage Through Public Method")
    void calculateTtl_NoState() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");
            when(redisService.getExamState("stateKey")).thenReturn(null);

            webSocketService.broadcastStudentJoined(1L, student, now);
        }
    }

    @Test
    @DisplayName("calculateTtl - State Present But EndAt Null (Forced)")
    void calculateTtl_StatePresent_EndAtNull() {
        try (MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
            redisUtils.when(() -> RedisUtils.examSessionState(anyLong())).thenReturn("stateKey");
            redisUtils.when(() -> RedisUtils.sessionActivityHistory(anyLong())).thenReturn("historyKey");

            // Using anonymous subclass to force getExamEndAt to return null
            SessionExamStateDTO state = new SessionExamStateDTO() {
                @Override
                public LocalDateTime getExamEndAt() {
                    return null;
                }
            };

            when(redisService.getExamState("stateKey")).thenReturn(state);

            webSocketService.broadcastStudentJoined(1L, student, now);
        }
    }
}
