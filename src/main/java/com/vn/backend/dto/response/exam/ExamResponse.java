package com.vn.backend.dto.response.exam;

import com.vn.backend.dto.response.subject.SubjectResponse;
import com.vn.backend.entities.Exam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamResponse {
    private Long examId;
    private SubjectResponse subject;
    private String title;
    private String description;

    public static ExamResponse fromEntity(Exam exam) {
        return ExamResponse.builder()
                .examId(exam.getExamId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .subject(SubjectResponse.fromEntity(exam.getSubject()))
                .build();
    }
}
