package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ClassScheduleRequest {

    private Long id;

    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.DAY_OF_WEEK)
    private DayOfWeek dayOfWeek;

    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.START_TIME)
    private LocalTime startTime;

    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.END_TIME)
    private LocalTime endTime;

    @NotAllowBlank(message = AppConst.MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.ROOM)
    private String room;

    public ClassScheduleRequestDTO toDTO(){
        return ClassScheduleRequestDTO.builder()
                .id(this.id)
                .dayOfWeek(this.dayOfWeek)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .room(this.room)
                .build();
    }
}
