package com.vn.backend.dto.response.classroom;

import com.vn.backend.entities.ClassSchedule;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ClassScheduleResponse {
    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;

    public static ClassScheduleResponse fromEntity(ClassSchedule entity) {
        if (entity == null) {
            return null;
        }
        ClassScheduleResponse response = new ClassScheduleResponse();
        response.setId(entity.getScheduleId());
        response.setDayOfWeek(entity.getDayOfWeek());
        response.setStartTime(entity.getStartTime());
        response.setEndTime(entity.getEndTime());
        response.setRoom(entity.getRoom());
        return response;
    }
}
