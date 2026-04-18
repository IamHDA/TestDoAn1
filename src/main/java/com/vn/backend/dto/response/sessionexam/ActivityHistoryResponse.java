package com.vn.backend.dto.response.sessionexam;

import com.vn.backend.dto.redis.ActivityHistoryRecordDTO;
import com.vn.backend.enums.EventType;
import com.vn.backend.enums.ViolationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityHistoryResponse {
    private EventType eventType;
    private Long sessionExamId;
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private LocalDateTime timestamp;

    private ViolationType violationType;
    private Integer violationCount;
    private String description;

    public static ActivityHistoryResponse fromDTO(Long sessionExamId, ActivityHistoryRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        ActivityHistoryResponseBuilder builder = ActivityHistoryResponse.builder()
                .eventType(EventType.valueOf(dto.getEventType()))
                .sessionExamId(sessionExamId)
                .studentId(dto.getStudentId())
                .studentCode(dto.getStudentCode())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .timestamp(LocalDateTime.parse(dto.getTimestamp()));

        if (dto.getViolationType() != null) {
            builder.violationType(ViolationType.valueOf(dto.getViolationType()))
                    .violationCount(dto.getViolationCount())
                    .description(dto.getDescription());
        }

        return builder.build();
    }
}
