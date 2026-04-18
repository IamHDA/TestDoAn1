package com.vn.backend.dto.request.submission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionGradeUpdateRequestDTO {
    private Double grade;
}
