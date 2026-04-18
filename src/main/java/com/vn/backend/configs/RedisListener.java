package com.vn.backend.configs;

import com.vn.backend.services.WebSocketService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisListener extends KeyExpirationEventMessageListener {

  // regex match exam:session:{sessionId}:active:{studentId}
  private static final Pattern STUDENT_ACTIVE_PATTERN =
      Pattern.compile("^exam:session:(\\d+):active:(\\d+)$");

  private final WebSocketService webSocketService;

  public RedisListener(RedisMessageListenerContainer listenerContainer,
      WebSocketService webSocketService) {
    super(listenerContainer);
    this.webSocketService = webSocketService;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String expiredKey = message.toString();

    Matcher matcher = STUDENT_ACTIVE_PATTERN.matcher(expiredKey);
    if (matcher.matches()) {
      Long sessionExamId = Long.valueOf(matcher.group(1));
      Long studentId = Long.valueOf(matcher.group(2));

      // Gửi offline event qua WebSocket tới teacher
      webSocketService.publishOffline(sessionExamId,studentId);
    } else {
      log.debug("Expired key ignored: {}", expiredKey);
    }
  }
}
