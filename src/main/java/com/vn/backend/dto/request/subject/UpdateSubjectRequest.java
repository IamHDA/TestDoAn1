package com.vn.backend.dto.request.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubjectRequest {
    
    @NotBlank(message = "Subject code is required")
    @Size(min = 2, max = 20, message = "Subject code must be between 2 and 20 characters")
    private String subjectCode;
    
    @NotBlank(message = "Subject name is required")
    @Size(min = 2, max = 255, message = "Subject name must be between 2 and 255 characters")
    private String subjectName;
}
