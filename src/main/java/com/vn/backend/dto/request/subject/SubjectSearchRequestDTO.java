package com.vn.backend.dto.request.subject;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectSearchRequestDTO {

    private String subjectName;
    private String subjectCode;
}
