package com.vn.backend.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Utils {

    public static List<Long> parseChoiceList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        // Nếu đã là List
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> {
                        if (item instanceof Long) {
                            return (Long) item;
                        } else if (item instanceof Integer) {
                            return ((Integer) item).longValue();
                        } else if (item instanceof String) {
                            return Long.parseLong((String) item);
                        }
                        throw new IllegalArgumentException("Invalid choice type: " + item.getClass());
                    })
                    .collect(Collectors.toList());
        }

        // Nếu là String JSON array
        if (value instanceof String str) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(str, new TypeReference<List<Long>>() {
                });
            } catch (Exception e) {
                log.error("Failed to parse choice list from string: {}", str, e);
                return new ArrayList<>();
            }
        }

        log.warn("Unknown choice list format: {}", value.getClass());
        return new ArrayList<>();
    }
}
