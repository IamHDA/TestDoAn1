package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.dto.redis.ActivityHistoryRecordDTO;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import com.vn.backend.dto.redis.StudentInfoDTO;
import com.vn.backend.dto.response.notification.NotificationWebSocketResponse;
import com.vn.backend.dto.websocket.*;
import com.vn.backend.entities.Notification;
import com.vn.backend.entities.User;
import com.vn.backend.enums.EventType;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.utils.JsonUtils;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.RedisUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketServiceImpl extends BaseService implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisService redisService;

    public WebSocketServiceImpl(
            MessageUtils messageUtils,
            SimpMessagingTemplate messagingTemplate,
            RedisService redisService) {
        super(messageUtils);
        this.messagingTemplate = messagingTemplate;
        this.redisService = redisService;
    }

    @Override
    public void sendNotificationToUser(Long userId, Notification notification) {
        try {
            NotificationWebSocketResponse dto = NotificationWebSocketResponse.fromEntity(notification);

            // Gửi đến /user/{userId}/queue/notifications
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    dto
            );

            log.info("Sent notification {} to user {}", notification.getNotificationId(), userId);
        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void broadcastStudentJoined(
            Long sessionExamId,
            User student,
            LocalDateTime joinedAt) {

        try {
            StudentJoinedEvent event = StudentJoinedEvent.builder()
                    .type(EventType.STUDENT_JOINED.toString())
                    .sessionExamId(sessionExamId)
                    .studentId(student.getId())
                    .studentCode(student.getCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .joinedAt(joinedAt)
                    .build();

            // Gửi đến monitoring (chỉ GV)
            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId + "/monitoring",
                    event
            );

            ActivityHistoryRecordDTO record = ActivityHistoryRecordDTO.builder()
                    .eventType(EventType.STUDENT_JOINED.toString())
                    .studentId(student.getId())
                    .studentCode(student.getCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .timestamp(joinedAt.toString())
                    .build();
            saveActivityToHistory(sessionExamId, record, calculateTtl(sessionExamId));

            log.info("Broadcast STUDENT_JOINED: sessionExamId={}, student={}",
                    sessionExamId, student.getEmail());

        } catch (Exception e) {
            log.error("Error broadcasting STUDENT_JOINED: sessionExamId={}, studentId={}, error={}",
                    sessionExamId, student.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void broadcastStudentDownloaded(
            Long sessionExamId,
            StudentInfoDTO student,
            LocalDateTime downloadedAt) {

        try {
            StudentDownloadedEvent event = StudentDownloadedEvent.builder()
                    .type(EventType.STUDENT_DOWNLOADED.toString())
                    .sessionExamId(sessionExamId)
                    .studentId(student.getStudentId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .downloadedAt(downloadedAt)
                    .answeredCount(0)
                    .build();

            // Gửi đến monitoring (chỉ GV)
            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId + "/monitoring",
                    event
            );

            ActivityHistoryRecordDTO record = ActivityHistoryRecordDTO.builder()
                    .eventType(EventType.STUDENT_DOWNLOADED.toString())
                    .studentId(student.getStudentId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .timestamp(downloadedAt.toString())
                    .build();
            saveActivityToHistory(sessionExamId, record, calculateTtl(sessionExamId));

            log.info("Broadcast STUDENT_DOWNLOADED: sessionExamId={}, student={}",
                    sessionExamId, student.getEmail());

        } catch (Exception e) {
            log.error("Error broadcasting STUDENT_DOWNLOADED: sessionExamId={}, studentId={}, error={}",
                    sessionExamId, student.getStudentId(), e.getMessage(), e);
        }
    }

    @Override
    public void broadcastStudentSubmitted(
            Long sessionExamId,
            StudentInfoDTO student,
            Double score,
            LocalDateTime submittedAt) {

        try {
            StudentSubmittedEvent event = StudentSubmittedEvent.builder()
                    .type(EventType.STUDENT_SUBMITTED.toString())
                    .sessionExamId(sessionExamId)
                    .studentId(student.getStudentId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .score(score)
                    .submittedAt(submittedAt)
                    .build();

            // Gửi đến monitoring (chỉ GV)
            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId + "/monitoring",
                    event
            );

            ActivityHistoryRecordDTO record = ActivityHistoryRecordDTO.builder()
                    .eventType(EventType.STUDENT_SUBMITTED.toString())
                    .studentId(student.getStudentId())
                    .studentCode(student.getStudentCode())
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .timestamp(submittedAt.toString())
                    .build();
            saveActivityToHistory(sessionExamId, record, calculateTtl(sessionExamId));


            log.info("Broadcast STUDENT_SUBMITTED: sessionExamId={}, student={}, score={}",
                    sessionExamId, student.getEmail(), score);

        } catch (Exception e) {
            log.error("Error broadcasting STUDENT_SUBMITTED: sessionExamId={}, studentId={}, error={}",
                    sessionExamId, student.getStudentId(), e.getMessage(), e);
        }
    }

    @Override
    public void broadcastGradingCompleted(
            Long sessionExamId,
            Integer totalGraded,
            LocalDateTime completedAt) {

        try {
            GradingCompletedEvent event = GradingCompletedEvent.builder()
                    .type(EventType.GRADING_COMPLETED.toString())
                    .sessionExamId(sessionExamId)
                    .totalGraded(totalGraded)
                    .completedAt(completedAt)
                    .build();

            // Gửi đến ALL (students và GV)
            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId,
                    event
            );

            log.info("Broadcast GRADING_COMPLETED: sessionExamId={}, totalGraded={}",
                    sessionExamId, totalGraded);

        } catch (Exception e) {
            log.error("Error broadcasting GRADING_COMPLETED: sessionExamId={}, error={}",
                    sessionExamId, e.getMessage(), e);
        }
    }

    @Override
    public void handleHeartbeat(Long sessionExamId, HeartbeatRequest request) {
        try {
            Long studentId = request.getStudentId();

            String activeKey = RedisUtils.sessionActiveStudents(sessionExamId, studentId);
            redisService.set(activeKey, studentId.toString(), AppConst.HEARTBEAT_CUTOFF_SECONDS,
                    TimeUnit.SECONDS);

            broadcastHeartbeat(sessionExamId, studentId);

            log.debug("Heartbeat received: sessionExamId={}, studentId={}",
                    sessionExamId, studentId);

        } catch (Exception e) {
            log.error("Error handling heartbeat: sessionExamId={}, studentId={}, error={}",
                    sessionExamId, request.getStudentId(), e.getMessage(), e);
        }
    }

    /**
     * Broadcast heartbeat event to monitoring topic
     */
    private void broadcastHeartbeat(Long sessionExamId, Long studentId) {
        try {
            // Get student info
            String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
            Map<String, Object> statusData = redisService.hGetAllAsString(statusKey);

            HeartbeatEvent event = HeartbeatEvent.builder()
                    .type(EventType.HEARTBEAT.toString())
                    .sessionExamId(sessionExamId)
                    .studentId(studentId)
                    .studentCode(parseString(statusData.get(FieldConst.STUDENT_CODE)))
                    .fullName(parseString(statusData.get(FieldConst.FULL_NAME)))
                    .build();

            // Broadcast to monitoring
            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId + "/monitoring",
                    event
            );

        } catch (Exception e) {
            log.error("Error broadcasting heartbeat: error={}", e.getMessage(), e);
        }
    }

    @Override
    public void handleViolation(
            Long sessionExamId,
            ViolationRequest violationRequest) {

        try {
            Long studentId = violationRequest.getStudentId();
            String stateKey = RedisUtils.examSessionState(sessionExamId);
            SessionExamStateDTO sessionExamStateDTO = redisService.getExamState(stateKey);
            // 1. Increment violation count in Redis
            String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
            Long currentViolations = redisService.hIncrement(
                    statusKey,
                    FieldConst.VIOLATIONS,
                    1L
            );

            // 2. Update violation type và description
            Map<String, Object> violationData = new HashMap<>();
            violationData.put(FieldConst.LAST_VIOLATION_TYPE, violationRequest.getViolationType());
            violationData.put(FieldConst.LAST_VIOLATION_AT, LocalDateTime.now());
            if (violationRequest.getDescription() != null) {
                violationData.put(FieldConst.LAST_VIOLATION_DESC, violationRequest.getDescription());
            }
            redisService.hSetAll(statusKey, violationData);

            // 3. Increment state violation count
            redisService.hIncrement(stateKey, FieldConst.VIOLATION_COUNT, 1L);

            // 4. Get student info to broadcast
            String studentCode = redisService.parseString(
                    redisService.hGet(statusKey, FieldConst.STUDENT_CODE));
            String fullName = redisService.parseString(
                    redisService.hGet(statusKey, FieldConst.FULL_NAME));
            String email = redisService.parseString(
                    redisService.hGet(statusKey, FieldConst.EMAIL));

            // 5. Broadcast violation event
            ViolationEvent event = ViolationEvent.builder()
                    .type(EventType.VIOLATION.toString())
                    .sessionExamId(sessionExamId)
                    .studentId(studentId)
                    .studentCode(studentCode)
                    .fullName(fullName)
                    .email(email)
                    .violationType(violationRequest.getViolationType().toString())
                    .violationCount(currentViolations.intValue())
                    .description(violationRequest.getDescription())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/exam/" + sessionExamId + "/monitoring",
                    event
            );

            ActivityHistoryRecordDTO activityRecord = ActivityHistoryRecordDTO.builder()
                    .eventType(EventType.VIOLATION.toString())
                    .studentId(studentId)
                    .studentCode(studentCode)
                    .fullName(fullName)
                    .email(email)
                    .timestamp(LocalDateTime.now().toString())
                    .violationType(violationRequest.getViolationType().toString())
                    .violationCount(currentViolations.intValue())
                    .description(violationRequest.getDescription())
                    .build();
            long ttl = Duration.between(LocalDateTime.now(), sessionExamStateDTO.getExamEndAt())
                    .getSeconds() + AppConst.TTL_BUFFER;
            saveActivityToHistory(sessionExamId, activityRecord, ttl);

            log.warn("Violation recorded: sessionExamId={}, studentId={}, type={}, count={}",
                    sessionExamId, studentId,
                    violationRequest.getViolationType(),
                    currentViolations);

        } catch (Exception e) {
            log.error("Error handling violation: sessionExamId={}, studentId={}, error={}",
                    sessionExamId, violationRequest.getStudentId(), e.getMessage(), e);
        }
    }

    @Override
    public void publishOffline(Long sessionExamId, Long studentId) {
        // Lấy thông tin student từ Redis
        String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
        String studentCode = redisService.parseString(
                redisService.hGet(statusKey, FieldConst.STUDENT_CODE));
        String fullName = redisService.parseString(
                redisService.hGet(statusKey, FieldConst.FULL_NAME));
        String email = redisService.parseString(
                redisService.hGet(statusKey, FieldConst.EMAIL));

        // Build event
        StudentOfflineEvent event = StudentOfflineEvent.builder()
                .type(EventType.STUDENT_OFFLINE.toString())
                .sessionExamId(sessionExamId)
                .studentId(studentId)
                .studentCode(studentCode)
                .fullName(fullName)
                .email(email)
                .build();
        // Broadcast tới teacher
        messagingTemplate.convertAndSend(
                "/topic/exam/" + sessionExamId + "/monitoring",
                event
        );

        ActivityHistoryRecordDTO record = ActivityHistoryRecordDTO.builder()
                .eventType(EventType.STUDENT_OFFLINE.toString())
                .studentId(studentId)
                .studentCode(studentCode)
                .fullName(fullName)
                .email(email)
                .timestamp(LocalDateTime.now().toString())
                .build();
        saveActivityToHistory(sessionExamId, record, calculateTtl(sessionExamId));
    }

    /**
     * Lưu activity vào Redis history
     */
    private void saveActivityToHistory(Long sessionExamId, ActivityHistoryRecordDTO record, long ttlSeconds) {
        String activityHistoryKey = RedisUtils.sessionActivityHistory(sessionExamId);
        redisService.lPushLeft(activityHistoryKey, JsonUtils.convertToJson(record));
        redisService.expire(activityHistoryKey, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Tính TTL dựa trên examEndAt
     */
    private long calculateTtl(Long sessionExamId) {
        String stateKey = RedisUtils.examSessionState(sessionExamId);
        SessionExamStateDTO state = redisService.getExamState(stateKey);
        if (state != null && state.getExamEndAt() != null) {
            return Duration.between(LocalDateTime.now(), state.getExamEndAt()).getSeconds() + AppConst.TTL_BUFFER;
        }
        return AppConst.TTL_BUFFER;
    }

    /**
     * Parse String from Object
     */
    public String parseString(Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }
}