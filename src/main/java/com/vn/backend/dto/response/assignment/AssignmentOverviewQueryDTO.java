package com.vn.backend.dto.response.assignment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignmentOverviewQueryDTO {
    private Long totalStudents;
    private Long submitted;
    private Long notSubmitted;
    private Long lateSubmissions;
    private Long graded;
    private Double avgGrade;
    private Double maxGrade;
    private Double minGrade;
}
