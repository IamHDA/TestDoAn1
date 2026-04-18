package com.vn.backend.dto.response.assignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentOverviewResponse {
    private Long totalStudents;
    private Long submitted;
    private Long notSubmitted;
    private Long lateSubmissions;
    private Long graded;
    private Double avgGrade;
    private Double maxGrade;
    private Double minGrade;
    private Double completionRate;

    public static AssignmentOverviewResponse fromDTO(AssignmentOverviewQueryDTO dto) {
        double completionRate = 0.0;
        if (dto.getTotalStudents() != null && dto.getTotalStudents() > 0) {
            // submitted + lateSubmissions xem như đã nộp
            long completed = (dto.getSubmitted() == null ? 0 : dto.getSubmitted())
                    + (dto.getLateSubmissions() == null ? 0 : dto.getLateSubmissions());
            completionRate = (double) completed / dto.getTotalStudents() * 100;
        }
        return AssignmentOverviewResponse.builder()
                .totalStudents(dto.getTotalStudents())
                .submitted(dto.getSubmitted())
                .notSubmitted(dto.getNotSubmitted())
                .lateSubmissions(dto.getLateSubmissions())
                .graded(dto.getGraded())
                .avgGrade(dto.getAvgGrade())
                .maxGrade(dto.getMaxGrade())
                .minGrade(dto.getMinGrade())
                .completionRate(completionRate)
                .build();
    }
}
