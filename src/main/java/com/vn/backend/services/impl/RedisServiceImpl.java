package com.vn.backend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.services.RedisService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void set(String key, Object value) {
    try {
      redisTemplate.opsForValue().set(key, value);
    } catch (Exception e) {
      log.error("Error setting key: {}", key, e);
      throw new RuntimeException("Redis SET operation failed", e);
    }
  }

  @Override
  public void set(String key, Object value, long timeout, TimeUnit unit) {
    try {
      redisTemplate.opsForValue().set(key, value, timeout, unit);
    } catch (Exception e) {
      log.error("Error setting key with expire: {}", key, e);
      throw new RuntimeException("Redis SET with expire operation failed", e);
    }
  }

  @Override
  public void setWithExpire(String key, Object value, long seconds) {
    set(key, value, seconds, TimeUnit.SECONDS);
  }

  @Override
  public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
    try {
      return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    } catch (Exception e) {
      log.error("Error setting key if absent: {}", key, e);
      throw new RuntimeException("Redis SETNX operation failed", e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> clazz) {
    try {
      Object value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return null;
      }
      // Không cần ObjectMapper nữa
      if (clazz.isInstance(value)) {
        return (T) value;
      }
      // fallback nếu cần convert
      return objectMapper.convertValue(value, clazz);
    } catch (Exception e) {
      log.error("Error getting key: {}", key, e);
      return null;
    }
  }

  @Override
  public <T> T get(String key, TypeReference<T> typeRef) {
    try {
      Object value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return null;
      }

      if (value instanceof String) {
        return objectMapper.readValue((String) value, typeRef);
      }

      return objectMapper.convertValue(value, typeRef);
    } catch (Exception e) {
      log.error("Error getting key: {}", key, e);
      return null;
    }
  }


  @Override
  public Object get(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("Error getting key: {}", key, e);
      return null;
    }
  }

  @Override
  public Boolean delete(String key) {
    try {
      return redisTemplate.delete(key);
    } catch (Exception e) {
      log.error("Error deleting key: {}", key, e);
      throw new RuntimeException("Redis DELETE operation failed", e);
    }
  }

  @Override
  public Long delete(String... keys) {
    try {
      return redisTemplate.delete(Arrays.asList(keys));
    } catch (Exception e) {
      log.error("Error deleting keys: {}", Arrays.toString(keys), e);
      throw new RuntimeException("Redis DELETE multiple keys operation failed", e);
    }
  }

  @Override
  public Boolean exists(String key) {
    try {
      return redisTemplate.hasKey(key);
    } catch (Exception e) {
      log.error("Error checking key exists: {}", key, e);
      return false;
    }
  }

  @Override
  public Boolean expire(String key, long timeout, TimeUnit unit) {
    try {
      return redisTemplate.expire(key, timeout, unit);
    } catch (Exception e) {
      log.error("Error setting expire for key: {}", key, e);
      throw new RuntimeException("Redis EXPIRE operation failed", e);
    }
  }

  @Override
  public Long getExpire(String key, TimeUnit unit) {
    try {
      return redisTemplate.getExpire(key, unit);
    } catch (Exception e) {
      log.error("Error getting expire for key: {}", key, e);
      return null;
    }
  }


  @Override
  public void hSet(String key, String field, Object value) {
    try {
      redisTemplate.opsForHash().put(key, field, value);
    } catch (Exception e) {
      log.error("Error setting hash field: {} {}", key, field, e);
      throw new RuntimeException("Redis HSET operation failed", e);
    }
  }

  @Override
  public Long hLen(String key) {
    try {
      return redisTemplate.opsForHash().size(key);
    } catch (Exception e) {
      log.error("Error getting hash length for key: {}", key, e);
      return 0L;
    }
  }

  @Override
  public void hSetAll(String key, Map<String, Object> map) {
    try {
      Map<String, Object> hashMap = new HashMap<>(map);
      redisTemplate.opsForHash().putAll(key, hashMap);
    } catch (Exception e) {
      log.error("Error setting hash all: {}", key, e);
      throw new RuntimeException("Redis HMSET operation failed", e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T hGet(String key, String field, Class<T> clazz) {
    try {
      Object value = redisTemplate.opsForHash().get(key, field);
      if (value == null) {
        return null;
      }

      if (clazz.isInstance(value)) {
        return (T) value;
      }

      if (value instanceof String) {
        return objectMapper.readValue((String) value, clazz);
      }

      return objectMapper.convertValue(value, clazz);
    } catch (Exception e) {
      log.error("Error getting hash field: {} {}", key, field, e);
      return null;
    }
  }

  @Override
  public Object hGet(String key, String field) {
    try {
      return redisTemplate.opsForHash().get(key, field);
    } catch (Exception e) {
      log.error("Error getting hash field: {} {}", key, field, e);
      return null;
    }
  }

  @Override
  public Map<Object, Object> hGetAll(String key) {
    try {
      return redisTemplate.opsForHash().entries(key);
    } catch (Exception e) {
      log.error("Error getting hash all: {}", key, e);
      return new HashMap<>();
    }
  }

  @Override
  public Map<String, Object> hGetAllAsString(String key) {
    Map<Object, Object> raw = hGetAll(key);
    return raw.entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().toString(),
            Map.Entry::getValue
        ));
  }

  @Override
  public Long hDelete(String key, String... fields) {
    try {
      return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    } catch (Exception e) {
      log.error("Error deleting hash fields: {} {}", key, Arrays.toString(fields), e);
      throw new RuntimeException("Redis HDEL operation failed", e);
    }
  }

  @Override
  public Boolean hExists(String key, String field) {
    try {
      return redisTemplate.opsForHash().hasKey(key, field);
    } catch (Exception e) {
      log.error("Error checking hash field exists: {} {}", key, field, e);
      return false;
    }
  }

  @Override
  public Long hIncrement(String key, String field, long delta) {
    try {
      return redisTemplate.opsForHash().increment(key, field, delta);
    } catch (Exception e) {
      log.error("Error incrementing hash field: {} {}", key, field, e);
      throw new RuntimeException("Redis HINCRBY operation failed", e);
    }
  }

  @Override
  public Double hIncrement(String key, String field, double delta) {
    try {
      return redisTemplate.opsForHash().increment(key, field, delta);
    } catch (Exception e) {
      log.error("Error incrementing hash field: {} {}", key, field, e);
      throw new RuntimeException("Redis HINCRBYFLOAT operation failed", e);
    }
  }

  @Override
  public Long hSize(String key) {
    try {
      return redisTemplate.opsForHash().size(key);
    } catch (Exception e) {
      log.error("Error getting hash size: {}", key, e);
      return 0L;
    }
  }

  @Override
  public Set<Object> hKeys(String key) {
    try {
      return redisTemplate.opsForHash().keys(key);
    } catch (Exception e) {
      log.error("Error getting hash keys: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public List<Object> hValues(String key) {
    try {
      return redisTemplate.opsForHash().values(key);
    } catch (Exception e) {
      log.error("Error getting hash values: {}", key, e);
      return new ArrayList<>();
    }
  }


  @Override
  public Long lPush(String key, Object... values) {
    try {
      return redisTemplate.opsForList().rightPushAll(key, values);
    } catch (Exception e) {
      log.error("Error pushing to list: {}", key, e);
      throw new RuntimeException("Redis RPUSH operation failed", e);
    }
  }

  @Override
  public Long lPushLeft(String key, Object... values) {
    try {
      return redisTemplate.opsForList().leftPushAll(key, values);
    } catch (Exception e) {
      log.error("Error pushing to list left: {}", key, e);
      throw new RuntimeException("Redis LPUSH operation failed", e);
    }
  }

  @Override
  public List<Object> lRange(String key, long start, long end) {
    try {
      return redisTemplate.opsForList().range(key, start, end);
    } catch (Exception e) {
      log.error("Error getting list range: {}", key, e);
      return new ArrayList<>();
    }
  }

  @Override
  public Long lSize(String key) {
    try {
      return redisTemplate.opsForList().size(key);
    } catch (Exception e) {
      log.error("Error getting list size: {}", key, e);
      return 0L;
    }
  }

  @Override
  public Object lIndex(String key, long index) {
    try {
      return redisTemplate.opsForList().index(key, index);
    } catch (Exception e) {
      log.error("Error getting list index: {} {}", key, index, e);
      return null;
    }
  }

  @Override
  public Long lRemove(String key, long count, Object value) {
    try {
      return redisTemplate.opsForList().remove(key, count, value);
    } catch (Exception e) {
      log.error("Error removing from list: {}", key, e);
      throw new RuntimeException("Redis LREM operation failed", e);
    }
  }

  @Override
  public Object lPop(String key) {
    try {
      return redisTemplate.opsForList().rightPop(key);
    } catch (Exception e) {
      log.error("Error popping from list: {}", key, e);
      return null;
    }
  }

  @Override
  public Object lPopLeft(String key) {
    try {
      return redisTemplate.opsForList().leftPop(key);
    } catch (Exception e) {
      log.error("Error popping from list left: {}", key, e);
      return null;
    }
  }

  @Override
  public Long sAdd(String key, Object... values) {
    try {
      return redisTemplate.opsForSet().add(key, values);
    } catch (Exception e) {
      log.error("Error adding to set: {}", key, e);
      throw new RuntimeException("Redis SADD operation failed", e);
    }
  }

  @Override
  public Long sRemove(String key, Object... values) {
    try {
      return redisTemplate.opsForSet().remove(key, values);
    } catch (Exception e) {
      log.error("Error removing from set: {}", key, e);
      throw new RuntimeException("Redis SREM operation failed", e);
    }
  }

  @Override
  public Set<Object> sMembers(String key) {
    try {
      return redisTemplate.opsForSet().members(key);
    } catch (Exception e) {
      log.error("Error getting set members: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public Boolean sIsMember(String key, Object value) {
    try {
      return redisTemplate.opsForSet().isMember(key, value);
    } catch (Exception e) {
      log.error("Error checking set member: {}", key, e);
      return false;
    }
  }

  @Override
  public Long sSize(String key) {
    try {
      return redisTemplate.opsForSet().size(key);
    } catch (Exception e) {
      log.error("Error getting set size: {}", key, e);
      return 0L;
    }
  }

  @Override
  public Boolean zAdd(String key, Object value, double score) {
    try {
      return redisTemplate.opsForZSet().add(key, value, score);
    } catch (Exception e) {
      log.error("Error adding to sorted set: {}", key, e);
      throw new RuntimeException("Redis ZADD operation failed", e);
    }
  }

  @Override
  public Long zRemove(String key, Object... values) {
    try {
      return redisTemplate.opsForZSet().remove(key, values);
    } catch (Exception e) {
      log.error("Error removing from sorted set: {}", key, e);
      throw new RuntimeException("Redis ZREM operation failed", e);
    }
  }

  @Override
  public Set<Object> zRange(String key, long start, long end) {
    try {
      return redisTemplate.opsForZSet().range(key, start, end);
    } catch (Exception e) {
      log.error("Error getting sorted set range: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public Set<Object> zRangeWithScores(String key, long start, long end) {
    try {
      Set<ZSetOperations.TypedTuple<Object>> tuples =
          redisTemplate.opsForZSet().rangeWithScores(key, start, end);

      if (tuples == null) {
        return new HashSet<>();
      }

      return tuples.stream()
          .map(ZSetOperations.TypedTuple::getValue)
          .collect(Collectors.toSet());
    } catch (Exception e) {
      log.error("Error getting sorted set range with scores: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public Set<Object> zReverseRange(String key, long start, long end) {
    try {
      return redisTemplate.opsForZSet().reverseRange(key, start, end);
    } catch (Exception e) {
      log.error("Error getting sorted set reverse range: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public Set<Object> zReverseRangeWithScores(String key, long start, long end) {
    try {
      Set<ZSetOperations.TypedTuple<Object>> tuples =
          redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

      if (tuples == null) {
        return new HashSet<>();
      }

      return tuples.stream()
          .map(ZSetOperations.TypedTuple::getValue)
          .collect(Collectors.toSet());
    } catch (Exception e) {
      log.error("Error getting sorted set reverse range with scores: {}", key, e);
      return new HashSet<>();
    }
  }

  @Override
  public Long zRemoveRangeByScore(String key, double min, double max) {
    try {
      return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    } catch (Exception e) {
      log.error("Error removing sorted set by score: {}", key, e);
      throw new RuntimeException("Redis ZREMRANGEBYSCORE operation failed", e);
    }
  }

  @Override
  public Long zSize(String key) {
    try {
      return redisTemplate.opsForZSet().size(key);
    } catch (Exception e) {
      log.error("Error getting sorted set size: {}", key, e);
      return 0L;
    }
  }

  @Override
  public Double zScore(String key, Object value) {
    try {
      return redisTemplate.opsForZSet().score(key, value);
    } catch (Exception e) {
      log.error("Error getting sorted set score: {} {}", key, value, e);
      return null;
    }
  }

  @Override
  public Double zIncrementScore(String key, Object value, double delta) {
    try {
      return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    } catch (Exception e) {
      log.error("Error incrementing sorted set score: {}", key, e);
      throw new RuntimeException("Redis ZINCRBY operation failed", e);
    }
  }

  @Override
  public Set<String> keys(String pattern) {
    try {
      return redisTemplate.keys(pattern);
    } catch (Exception e) {
      log.error("Error getting keys by pattern: {}", pattern, e);
      return new HashSet<>();
    }
  }

  @Override
  public Long deleteByPattern(String pattern) {
    try {
      Set<String> keys = redisTemplate.keys(pattern);
      if (keys == null || keys.isEmpty()) {
        return 0L;
      }
      return redisTemplate.delete(keys);
    } catch (Exception e) {
      log.error("Error deleting keys by pattern: {}", pattern, e);
      throw new RuntimeException("Redis DELETE by pattern operation failed", e);
    }
  }

  @Override
  public Long parseLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      log.warn("Cannot parse Long from: {}", value);
      return null;
    }
  }

  @Override
  public Double parseDouble(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Double) {
      return (Double) value;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(value));
    } catch (NumberFormatException e) {
      log.warn("Cannot parse Double from: {}", value);
      return null;
    }
  }

  @Override
  public String parseString(Object value) {
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }

  @Override
  public String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.error("Error serializing object to JSON", e);
      throw new RuntimeException("JSON serialization failed", e);
    }
  }

  @Override
  public <T> T fromJson(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      log.error("Error deserializing JSON to object", e);
      throw new RuntimeException("JSON deserialization failed", e);
    }
  }

  /**
   * Get members of sorted set by score range
   *
   * @param key Redis sorted set key
   * @param min Min score (inclusive)
   * @param max Max score (inclusive)
   * @return Set of members
   */
  @Override
  public Set<Object> zRangeByScore(String key, double min, double max) {
    try {
      return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    } catch (Exception e) {
      log.error("Error getting zRangeByScore for key: {}, min: {}, max: {}",
          key, min, max, e);
      return new HashSet<>();
    }
  }

  /**
   * Get exam state from flat hash structure
   */
  public SessionExamStateDTO getExamState(String stateKey) {
    Map<Object, Object> stateMap = redisTemplate.opsForHash().entries(stateKey);

    if (stateMap.isEmpty()) {
      return null;
    }

    try {
      return SessionExamStateDTO.builder()
          .instructorId(parseLong(stateMap.get(FieldConst.INSTRUCTOR_ID)))
          .sessionExamId(parseLong(stateMap.get(FieldConst.SESSION_EXAM_ID)))
          .title(parseString(stateMap.get(FieldConst.TITLE)))
          .description(parseString(stateMap.get(FieldConst.DESCRIPTION)))
          .questionOrderMode(parseQuestionOrderMode(stateMap.get(FieldConst.QUESTION_ORDER_MODE)))
          .isInstantlyResult(parseBoolean(stateMap.get(FieldConst.IS_INSTANTLY_RESULT)))
          .countdownStartAt(parseLocalDateTime(stateMap.get(FieldConst.COUNTDOWN_START_AT)))
          .duration(parseLong(stateMap.get(FieldConst.DURATION)))
          .totalStudents(parseInt(stateMap.get(FieldConst.TOTAL_STUDENTS)))
          .joinedCount(parseInt(stateMap.get(FieldConst.JOINED_COUNT)))
          .downloadedCount(parseInt(stateMap.get(FieldConst.DOWNLOADED_COUNT)))
          .submittedCount(parseInt(stateMap.get(FieldConst.SUBMITTED_COUNT)))
          .violationCount(parseInt(stateMap.get(FieldConst.VIOLATION_COUNT)))
          .build();
    } catch (Exception e) {
      log.error("Error parsing exam state from Redis: {}", stateKey, e);
      return null;
    }
  }

  /**
   * Parse QuestionOrderMode from Redis value
   */
  private QuestionOrderMode parseQuestionOrderMode(Object value) {
    if (value == null) {
      return QuestionOrderMode.SEQUENTIAL;
    }

    try {
      return QuestionOrderMode.valueOf(value.toString());
    } catch (Exception e) {
      log.error("Error parsing QuestionOrderMode: {}", value, e);
      return QuestionOrderMode.SEQUENTIAL;
    }
  }

  /**
   * Parse LocalDateTime from Redis value
   */
  public LocalDateTime parseLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDateTime) {
      return (LocalDateTime) value;
    }

    try {
      return LocalDateTime.parse(value.toString());
    } catch (Exception e) {
      log.error("Error parsing LocalDateTime: {}", value, e);
      return null;
    }
  }

  /**
   * Parse Integer from Object (handle various types)
   */
  public Integer parseInt(Object value) {
    if (value == null) {
      return 0;
    }

    if (value instanceof Integer) {
      return (Integer) value;
    }

    if (value instanceof Number) {
      return ((Number) value).intValue();
    }

    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      log.error("Error parsing Integer: {}", value, e);
      return 0;
    }
  }

  /**
   * Parse Boolean from Object
   */
  public Boolean parseBoolean(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof Boolean) {
      return (Boolean) value;
    }

    try {
      return Boolean.parseBoolean(value.toString());
    } catch (Exception e) {
      log.error("Error parsing Boolean: {}", value, e);
      return false;
    }
  }
}