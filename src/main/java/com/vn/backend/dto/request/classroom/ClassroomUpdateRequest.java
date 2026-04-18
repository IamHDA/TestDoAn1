package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.enums.ClassCodeStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.utils.EnumUtils;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class ClassroomUpdateRequest {

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_NAME)
    private String className;

    private String description;

    private String coverImageUrl;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASSROOM_STATUS)
    @ValidEnum(enumClass = ClassroomStatus.class, fieldName = FieldConst.CLASSROOM_STATUS, message = MessageConst.VALUE_OUT_OF_RANGE)
    private String classroomStatus;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_CODE_STATUS)
    @ValidEnum(enumClass = ClassCodeStatus.class, fieldName = FieldConst.CLASSROOM_STATUS, message = MessageConst.VALUE_OUT_OF_RANGE)
    private String classCodeStatus;

    @Valid
    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_SCHEDULES)
    private List<ClassScheduleRequest> classSchedules;

    public ClassroomUpdateRequestDTO toDTO() {
        return ClassroomUpdateRequestDTO.builder()
                .className(this.className)
                .description(this.description)
                .coverImageUrl(this.coverImageUrl)
                .classroomStatus(EnumUtils.fromString(ClassroomStatus.class, this.classroomStatus))
                .classCodeStatus(EnumUtils.fromString(ClassCodeStatus.class, this.classCodeStatus))
                .classScheduleRequestDTOS(
                        classSchedules.stream()
                                .map(ClassScheduleRequest::toDTO)
                                .toList()
                )
                .build();
    }
}
