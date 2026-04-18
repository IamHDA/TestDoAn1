package com.vn.backend.dto.request.subject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectCreateRequestDTO {
    private String subjectCode;
    private String subjectName;
}
