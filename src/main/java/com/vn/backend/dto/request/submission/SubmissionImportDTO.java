package com.vn.backend.dto.request.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionImportDTO {

    private String username;
    private String fullName;
    private String code;
    private Double grade;
}
