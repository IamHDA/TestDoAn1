package com.vn.backend.dto.request.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamUpdateRequestDTO {
    private Long subjectId;
    private String title;
    private String description;
}
