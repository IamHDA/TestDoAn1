package com.vn.backend.dto.response.classroom;

import com.vn.backend.dto.response.subject.SubjectResponse;
import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassroomSearchResponse {
    private Long classroomId;
    private String className;
    private SubjectResponse subject;
    private String coverImageUrl;
    private String classroomStatus;
    private UserInfoResponse teacher;
    private Long memberCount;
    private Long assignmentCount;
    private LocalDateTime createdAt;

    public static ClassroomSearchResponse fromDTO(ClassroomSearchQueryDTO dto) {
        ClassroomSearchResponse classroomSearchResponse = ModelMapperUtils.mapTo(dto, ClassroomSearchResponse.class);
        classroomSearchResponse.setSubject(ModelMapperUtils.mapTo(dto.getSubject(), SubjectResponse.class));
        classroomSearchResponse.setTeacher(
                UserInfoResponse.builder()
                        .id(dto.getTeacherId())
                        .username(dto.getUserName())
                        .fullName(dto.getFullName())
                        .avatarUrl(dto.getAvatarUrl())
                        .build()
        );
        return classroomSearchResponse;
    }
}
