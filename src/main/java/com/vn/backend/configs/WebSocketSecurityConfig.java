package com.vn.backend.configs;


import com.vn.backend.configs.jwt.JwtTokenProvider;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.services.RedisService;
import com.vn.backend.utils.RedisUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

  // Patterns for exam destinations
  private static final Pattern EXAM_TOPIC_PATTERN = Pattern.compile("^/topic/exam/(\\d+)$");
  private static final Pattern EXAM_MONITORING_PATTERN = Pattern.compile(
      "^/topic/exam/(\\d+)/monitoring$");
  private static final Pattern EXAM_SYNC_PATTERN = Pattern.compile("^/topic/exam/(\\d+)/sync$");
  private final JwtTokenProvider jwtTokenProvider;
  private final UserDetailsService userDetailsService;
  private final RedisService redisService;
  private final StudentSessionExamRepository studentSessionExamRepository;

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
            StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          handleConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
          handleSubscribe(accessor);
        }

        return message;
      }
    });
  }

  /**
   * Xử lý khi client kết nối
   */
  private void handleConnect(StompHeaderAccessor accessor) {
    String authToken = accessor.getFirstNativeHeader("Authorization");

    if (authToken != null && authToken.startsWith("Bearer ")) {
      String token = authToken.substring(7);

      try {
        if (jwtTokenProvider.validateToken(token)) {
          String username = jwtTokenProvider.getUsernameFromJwt(token);
          Long userId = jwtTokenProvider.getUserIdFromJwt(token);
          UserDetails userDetails = userDetailsService.loadUserByUsername(username);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userId,
                  null,
                  userDetails.getAuthorities()
              );

          SecurityContextHolder.getContext().setAuthentication(authentication);
          accessor.setUser(authentication);

          log.info("WebSocket authenticated user: {}", username);
        }
      } catch (Exception e) {
        log.error("WebSocket authentication failed: {}", e.getMessage());
      }
    } else {
      log.warn("WebSocket connection without Authorization header");
    }
  }


  private void handleSubscribe(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();

    if (destination == null) {
      return;
    }

    log.info("Client SUBSCRIBE to: {}", destination);

    // Validate classroom topic
    if (destination.startsWith("/topic/classroom/")) {
      validateClassroomSubscription(accessor, destination);
    }

    // Validate exam topics
    if (destination.startsWith("/topic/exam/")) {
      validateExamSubscription(accessor, destination);
    }
  }

  /**
   * Validate classroom subscription
   */
  private void validateClassroomSubscription(StompHeaderAccessor accessor, String destination) {
    if (accessor.getUser() == null) {
      log.warn("Unauthorized classroom subscription (no user): {}", destination);
      throw new SecurityException("Authentication required");
    }

    // TODO: Implement classroom authorization
    // Long classroomId = extractClassroomId(destination);
    // if (!canAccessClassroom(userId, classroomId)) {
    //     throw new SecurityException("Access denied");
    // }

    log.debug("Classroom subscription authorized: {}", destination);
  }

  /**
   * Validate exam subscription Kiểm tra quyền truy cập dựa trên JWT và Session Token
   */
  private void validateExamSubscription(StompHeaderAccessor accessor, String destination) {
    // Phải có user (authenticated với JWT)
    if (accessor.getUser() == null) {
      log.warn("Unauthorized exam subscription (no user): {}", destination);
      throw new SecurityException("Authentication required");
    }

    Long userId = getUserIdFromAccessor(accessor);

    // Check exam topic pattern
    Matcher examMatcher = EXAM_TOPIC_PATTERN.matcher(destination);
    Matcher monitoringMatcher = EXAM_MONITORING_PATTERN.matcher(destination);
    Matcher syncMatcher = EXAM_SYNC_PATTERN.matcher(destination);

    Long sessionExamId = null;

    if (examMatcher.matches()) {
      sessionExamId = Long.parseLong(examMatcher.group(1));
      validateExamTopicAccess(userId, sessionExamId, destination);
    } else if (monitoringMatcher.matches()) {
      sessionExamId = Long.parseLong(monitoringMatcher.group(1));
      validateMonitoringTopicAccess(userId, sessionExamId, destination);
    } else if (syncMatcher.matches()) {
      sessionExamId = Long.parseLong(syncMatcher.group(1));
      validateSyncTopicAccess(userId, sessionExamId, destination);
    } else {
      log.warn("Unknown exam destination pattern: {}", destination);
      throw new SecurityException("Invalid exam destination");
    }
  }

  /**
   * Validate /topic/exam/{sessionExamId} Student cần có session token
   */
  private void validateExamTopicAccess(Long userId, Long sessionExamId, String destination) {

    String studentsKey = RedisUtils.sessionStudents(sessionExamId);
    Boolean isInSession = redisService.sIsMember(studentsKey, String.valueOf(userId));

    if (Boolean.FALSE.equals(isInSession)) {
      if (!studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(
          sessionExamId, userId
      )) {
        log.warn("User {} not in exam session {}", userId, sessionExamId);
        throw new SecurityException("Not enrolled in this exam session");
      }
    }

    log.debug("User {} authorized to subscribe exam topic: {}", userId, destination);
  }

  /**
   * Validate /topic/exam/{sessionExamId}/monitoring Chỉ instructor mới được subscribe
   */
  private void validateMonitoringTopicAccess(Long userId, Long sessionExamId, String destination) {
    // Lấy instructorId từ exam session state
    String stateKey = RedisUtils.examSessionState(sessionExamId);
    Object instructorIdObj = redisService.hGet(stateKey, "instructorId");

    if (instructorIdObj == null) {
      log.warn("Exam session {} not found", sessionExamId);
      throw new SecurityException("Exam session not found");
    }

    Long instructorId = redisService.parseLong(instructorIdObj);

    if (!userId.equals(instructorId)) {
      log.warn("User {} denied access to monitoring topic (not instructor)", userId);
      throw new SecurityException("Only instructor can access monitoring");
    }

    log.debug("Instructor {} authorized to subscribe monitoring topic: {}", userId, destination);
  }

  /**
   * Validate /topic/exam/{sessionExamId}/sync Tất cả user trong session đều được subscribe (cho
   * time sync)
   */
  private void validateSyncTopicAccess(Long userId, Long sessionExamId, String destination) {
    // Kiểm tra user có trong session không
    String studentsKey = RedisUtils.sessionStudents(sessionExamId);
    Boolean isInSession = redisService.sIsMember(studentsKey, String.valueOf(userId));

    if (Boolean.FALSE.equals(isInSession)) {
      log.warn("User {} not in exam session {} (sync topic)", userId, sessionExamId);
      throw new SecurityException("Not enrolled in this exam session");
    }

    log.debug("User {} authorized to subscribe sync topic: {}", userId, destination);
  }

  /**
   * Extract userId from StompHeaderAccessor
   */
  private Long getUserIdFromAccessor(StompHeaderAccessor accessor) {
    if (accessor.getUser() == null) {
      throw new SecurityException("User not authenticated");
    }

    if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken) {
      UsernamePasswordAuthenticationToken auth =
          (UsernamePasswordAuthenticationToken) accessor.getUser();

      Object principal = auth.getPrincipal();

      if (principal instanceof Long) {
        return (Long) principal;
      }
    }

    throw new SecurityException("Invalid user principal");
  }
}
