package com.vn.backend.dto.response.classroom;

import com.vn.backend.dto.response.subject.SubjectResponse;
import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.entities.Classroom;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

@Data
public class ClassroomHeaderResponse {

    private Long classroomId;
    private String className;
    private SubjectResponse subject;
    private String coverImageUrl;
    private UserInfoResponse teacher;

    public static ClassroomHeaderResponse fromEntity(Classroom entity) {
        ClassroomHeaderResponse classroomHeaderResponse = ModelMapperUtils.mapTo(entity,
                ClassroomHeaderResponse.class);
        classroomHeaderResponse.setTeacher(UserInfoResponse.fromEntity(entity.getTeacher()));
        classroomHeaderResponse.setSubject(ModelMapperUtils.mapTo(entity.getSubject(), SubjectResponse.class));
        return classroomHeaderResponse;
    }

}
