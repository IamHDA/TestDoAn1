package com.vn.backend.dto.response.subject;

import com.vn.backend.entities.Subject;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

@Data
public class SubjectSearchResponse {

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    public static SubjectSearchResponse fromEntity(Subject subject) {
        return ModelMapperUtils.mapTo(subject, SubjectSearchResponse.class);
    }
}
