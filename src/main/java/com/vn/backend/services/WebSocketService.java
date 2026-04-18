package com.vn.backend.services;

import com.vn.backend.dto.redis.StudentInfoDTO;
import com.vn.backend.dto.websocket.HeartbeatRequest;
import com.vn.backend.dto.websocket.ViolationRequest;
import com.vn.backend.entities.Notification;
import com.vn.backend.entities.User;
import java.time.LocalDateTime;

public interface WebSocketService {

  void sendNotificationToUser(Long userId, Notification notification);

  /**
   * Broadcast STUDENT_JOINED event Gửi đến: /topic/exam/{sessionExamId}/monitoring (chỉ GV)
   */
  void broadcastStudentJoined(
      Long sessionExamId,
      User student,
      LocalDateTime joinedAt
  );

  /**
   * Broadcast STUDENT_DOWNLOADED event Gửi đến: /topic/exam/{sessionExamId}/monitoring (chỉ GV)
   */
  void broadcastStudentDownloaded(
      Long sessionExamId,
      StudentInfoDTO student,
      LocalDateTime downloadedAt
  );

  /**
   * Broadcast STUDENT_SUBMITTED event Gửi đến: /topic/exam/{sessionExamId}/monitoring (chỉ GV)
   */
  void broadcastStudentSubmitted(
      Long sessionExamId,
      StudentInfoDTO student,
      Double score,
      LocalDateTime submittedAt);

  /**
   * Broadcast GRADING_COMPLETED event Gửi đến: /topic/exam/{sessionExamId}
   */
  void broadcastGradingCompleted(
      Long sessionExamId,
      Integer totalGraded,
      LocalDateTime completedAt
  );

  /**
   * Handle heartbeat from student
   */
  void handleHeartbeat(
      Long sessionExamId,
      HeartbeatRequest heartbeatRequest
  );

  /**
   * Handle violation from student
   */
  void handleViolation(
      Long sessionExamId,
      ViolationRequest violationRequest
  );

  void publishOffline(Long sessionExamId, Long studentId);
}
