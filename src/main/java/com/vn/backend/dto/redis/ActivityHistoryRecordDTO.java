package com.vn.backend.dto.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityHistoryRecordDTO {
    private String eventType;

    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String timestamp;

    private String violationType;
    private Integer violationCount;
    private String description;
}