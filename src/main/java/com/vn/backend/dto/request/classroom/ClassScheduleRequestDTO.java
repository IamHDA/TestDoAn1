package com.vn.backend.dto.request.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class ClassScheduleRequestDTO {
    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
}
