package com.vn.backend.controllers;

import com.vn.backend.dto.websocket.HeartbeatRequest;
import com.vn.backend.dto.websocket.ViolationRequest;
import com.vn.backend.services.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

  private final WebSocketService webSocketService;

  /**
   * Handle heartbeat from student Client sends to: /session-exams/{sessionExamId}/heartbeat
   */
  @MessageMapping("/session-exams/{sessionExamId}/heartbeat")
  public void handleHeartbeat(
      @DestinationVariable Long sessionExamId,
      @Payload HeartbeatRequest request) {

    log.debug("Received heartbeat: sessionExamId={}, studentId={}",
        sessionExamId, request.getStudentId());

    webSocketService.handleHeartbeat(sessionExamId, request);
  }

  /**
   * Handle violation from student Client sends to: /session-exams/{sessionExamId}/violation
   */
  @MessageMapping("/session-exams/{sessionExamId}/violation")
  public void handleViolation(
      @DestinationVariable Long sessionExamId,
      @Payload ViolationRequest request) {

    log.warn("Received violation: sessionExamId={}, studentId={}, type={}",
        sessionExamId, request.getStudentId(), request.getViolationType());

    webSocketService.handleViolation(sessionExamId, request);
  }
}