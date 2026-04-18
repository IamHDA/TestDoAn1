package com.vn.backend.dto.request.exam;

import com.vn.backend.entities.Exam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamCreateRequestDTO {
    private Long subjectId;
    private String title;
    private String description;
    private Long createdBy;

    public Exam toEntity(){
        return Exam.builder()
                .subjectId(this.subjectId)
                .title(this.title)
                .description(this.description)
                .createdBy(this.createdBy)
                .build();
    }
}
