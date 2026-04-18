package com.vn.backend.dto.request.classroom;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.*;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClassroomCreateRequest {

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_NAME)
    private String className;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SUBJECT_ID)
    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.SUBJECT_ID)
    private String subjectId;

    private String description;

    private String coverImageUrl;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.START_DATE)
    private LocalDate startDate;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.END_DATE)
    private LocalDate endDate;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.COURSE_OUTLINE_URL)
    private String courseOutlineUrl;

    @Valid
    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.CLASS_SCHEDULES)
    private List<ClassScheduleRequest> classSchedules;

    // description for request create classroom
    private String requestDescription;

    public ClassroomCreateRequestDTO toDTO(){
        return ClassroomCreateRequestDTO
                .builder()
                .className(this.className)
                .subjectId(Long.parseLong(this.subjectId))
                .description(this.description)
                .coverImageUrl(this.coverImageUrl)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .courseOutlineUrl(this.courseOutlineUrl)
                .classScheduleRequestDTOS(
                    classSchedules.stream()
                            .map(ClassScheduleRequest::toDTO)
                            .toList()
                )
                .requestDescription(this.requestDescription)
                .build();
    }
}
