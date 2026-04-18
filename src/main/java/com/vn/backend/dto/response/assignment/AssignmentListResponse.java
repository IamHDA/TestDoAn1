package com.vn.backend.dto.response.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentListResponse {
    private Long assignmentId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueDate;
}
