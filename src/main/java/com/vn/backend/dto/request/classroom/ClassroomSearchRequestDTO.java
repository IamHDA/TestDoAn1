package com.vn.backend.dto.request.classroom;

import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ClassroomStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSearchRequestDTO {
    private String teacherName;
    private String className;
    private Long subjectId;
    private ClassroomStatus classroomStatus;
    private Long userId;
    private ClassMemberStatus classMemberStatus;
}
