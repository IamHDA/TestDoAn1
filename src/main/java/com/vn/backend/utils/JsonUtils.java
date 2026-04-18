package com.vn.backend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {

  /**
   * Convert object to JSON string
   */
  public static String convertToJson(Object object) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("Error converting to JSON", e);
      return null;
    }
  }

  /**
   * Parse JSON string to object
   */
  public static <T> T parseFromJson(String json, Class<T> clazz) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      log.error("Error parsing JSON", e);
      return null;
    }
  }

}
