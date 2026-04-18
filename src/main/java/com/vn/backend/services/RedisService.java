package com.vn.backend.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vn.backend.dto.redis.SessionExamStateDTO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface RedisService {

  // ==================== STRING OPERATIONS ====================

  /**
   * Set giá trị String
   */
  void set(String key, Object value);

  /**
   * Set giá trị String với TTL
   */
  void set(String key, Object value, long timeout, TimeUnit unit);

  /**
   * Set giá trị String với TTL (seconds)
   */
  void setWithExpire(String key, Object value, long seconds);

  /**
   * Set if absent (NX)
   */
  Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

  /**
   * Get giá trị String
   */
  <T> T get(String key, Class<T> clazz);

  <T> T get(String key, TypeReference<T> typeRef);

  /**
   * Get giá trị String (generic type)
   */
  Object get(String key);

  /**
   * Delete key
   */
  Boolean delete(String key);

  /**
   * Delete multiple keys
   */
  Long delete(String... keys);

  /**
   * Check key exists
   */
  Boolean exists(String key);

  /**
   * Set expire time
   */
  Boolean expire(String key, long timeout, TimeUnit unit);

  /**
   * Get TTL
   */
  Long getExpire(String key, TimeUnit unit);

  // ==================== HASH OPERATIONS ====================

  /**
   * Set Hash field
   */
  void hSet(String key, String field, Object value);

  /**
   * Get the number of fields in a hash
   *
   * @param key Redis key
   * @return Number of fields in the hash, or 0 if key doesn't exist
   */
  Long hLen(String key);

  /**
   * Set multiple Hash fields
   */
  void hSetAll(String key, Map<String, Object> map);

  /**
   * Get Hash field
   */
  <T> T hGet(String key, String field, Class<T> clazz);

  /**
   * Get Hash field (generic)
   */
  Object hGet(String key, String field);

  /**
   * Get all Hash fields and values
   */
  Map<Object, Object> hGetAll(String key);

  Map<String, Object> hGetAllAsString(String key);

  /**
   * Delete Hash field
   */
  Long hDelete(String key, String... fields);

  /**
   * Check Hash field exists
   */
  Boolean hExists(String key, String field);

  /**
   * Increment Hash field (Long)
   */
  Long hIncrement(String key, String field, long delta);

  /**
   * Increment Hash field (Double)
   */
  Double hIncrement(String key, String field, double delta);

  /**
   * Get Hash size
   */
  Long hSize(String key);

  /**
   * Get all Hash keys
   */
  Set<Object> hKeys(String key);

  /**
   * Get all Hash values
   */
  List<Object> hValues(String key);

  // ==================== LIST OPERATIONS ====================

  /**
   * Push to list (right)
   */
  Long lPush(String key, Object... values);

  /**
   * Push to list (left)
   */
  Long lPushLeft(String key, Object... values);

  /**
   * Get list range
   */
  List<Object> lRange(String key, long start, long end);

  /**
   * Get list size
   */
  Long lSize(String key);

  /**
   * Get list element at index
   */
  Object lIndex(String key, long index);

  /**
   * Remove list elements
   */
  Long lRemove(String key, long count, Object value);

  /**
   * Pop from list (right)
   */
  Object lPop(String key);

  /**
   * Pop from list (left)
   */
  Object lPopLeft(String key);

  // ==================== SET OPERATIONS ====================

  /**
   * Add to set
   */
  Long sAdd(String key, Object... values);

  /**
   * Remove from set
   */
  Long sRemove(String key, Object... values);

  /**
   * Get all set members
   */
  Set<Object> sMembers(String key);

  /**
   * Check set member exists
   */
  Boolean sIsMember(String key, Object value);

  /**
   * Get set size
   */
  Long sSize(String key);

  // ==================== SORTED SET OPERATIONS ====================

  /**
   * Add to sorted set
   */
  Boolean zAdd(String key, Object value, double score);

  /**
   * Remove from sorted set
   */
  Long zRemove(String key, Object... values);

  /**
   * Get sorted set range (by index)
   */
  Set<Object> zRange(String key, long start, long end);

  /**
   * Get sorted set range with scores
   */
  Set<Object> zRangeWithScores(String key, long start, long end);

  /**
   * Get sorted set reverse range
   */
  Set<Object> zReverseRange(String key, long start, long end);

  /**
   * Get sorted set reverse range with scores
   */
  Set<Object> zReverseRangeWithScores(String key, long start, long end);

  /**
   * Remove sorted set by score range
   */
  Long zRemoveRangeByScore(String key, double min, double max);

  /**
   * Get sorted set size
   */
  Long zSize(String key);

  /**
   * Get score of member
   */
  Double zScore(String key, Object value);

  /**
   * Increment score
   */
  Double zIncrementScore(String key, Object value, double delta);

  // ==================== KEY PATTERN OPERATIONS ====================

  /**
   * Get keys by pattern
   */
  Set<String> keys(String pattern);

  /**
   * Delete keys by pattern
   */
  Long deleteByPattern(String pattern);

  // ==================== UTILITY METHODS ====================

  /**
   * Parse Long from Object
   */
  Long parseLong(Object value);

  /**
   * Parse Double from Object
   */
  Double parseDouble(Object value);

  /**
   * Parse String from Object
   */
  String parseString(Object value);

  /**
   * Serialize object to JSON string
   */
  String toJson(Object obj);

  /**
   * Deserialize JSON string to object
   */
  <T> T fromJson(String json, Class<T> clazz);

  Set<Object> zRangeByScore(String key, double min, double max);

  SessionExamStateDTO getExamState(String stateKey);
}