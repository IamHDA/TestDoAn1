package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.ClassCodeStatus;
import com.vn.backend.enums.ClassroomStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ClassroomCreateRequestDTO {

    private String className;

    private Long subjectId;

    private String section;

    private String room;

    private String description;

    private String coverImageUrl;

    private Long teacherId;

    private String classCode;

    private ClassroomStatus classroomStatus;

    private ClassCodeStatus classCodeStatus;

    private LocalDate startDate;

    private LocalDate endDate;

    private String courseOutlineUrl;

    private List<ClassScheduleRequestDTO> classScheduleRequestDTOS;

    private String requestDescription;
}
