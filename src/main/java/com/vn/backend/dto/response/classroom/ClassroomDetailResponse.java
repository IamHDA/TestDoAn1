package com.vn.backend.dto.response.classroom;

import com.vn.backend.dto.response.subject.SubjectResponse;
import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClassroomDetailResponse {

    private Long classroomId;
    private String classCode;
    private String className;
    private SubjectResponse subject;
    private String description;
    private String coverImageUrl;
    private String classroomStatus;
    private String classCodeStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String courseOutlineUrl;
    private UserInfoResponse teacher;
    private List<ClassScheduleResponse> schedules;

    public static ClassroomDetailResponse fromEntity(Classroom entity) {
        ClassroomDetailResponse response = ModelMapperUtils.mapTo(entity,
                ClassroomDetailResponse.class);
        response.setTeacher(UserInfoResponse.fromEntity(entity.getTeacher()));
        response.setSubject(ModelMapperUtils.mapTo(entity.getSubject(), SubjectResponse.class));
        response.setSchedules(
                entity.getSchedules().stream()
                        .map(ClassScheduleResponse::fromEntity)
                        .toList()
        );
        return response;
    }
}
