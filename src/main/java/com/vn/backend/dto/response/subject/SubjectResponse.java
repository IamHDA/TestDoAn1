package com.vn.backend.dto.response.subject;

import com.vn.backend.entities.Subject;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

@Data
public class SubjectResponse {
    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    public static SubjectResponse fromEntity(Subject subject) {
        return ModelMapperUtils.mapTo(subject, SubjectResponse.class);
    }
}
