package com.vn.backend.dto.response.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {

    @Schema(description = "Assignment ID", example = "1")
    private Long assignmentId;

    @Schema(description = "Assignment title", example = "Math Homework 1")
    private String title;

    @Schema(description = "Assignment content", example = "Complete exercises 1-10 from chapter 5")
    private String content;

    @Schema(description = "Classroom ID", example = "1")
    private Long classroomId;

    @Schema(description = "Creator user ID", example = "1")
    private Long createdBy;

    @Schema(description = "Maximum score for this assignment", example = "100")
    private Long maxScore;

    @Schema(description = "Due date for the assignment", example = "2024-01-31T23:59:59")
    private LocalDateTime dueDate;

    @Schema(description = "Whether submission is closed after due date", example = "true")
    private Boolean submissionClosed;

    @Schema(description = "Whether comments are allowed", example = "true")
    private Boolean allowComments;

    @Schema(description = "Assignment creation date", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Assignment last update date", example = "2024-01-20T14:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Whether current user can edit this assignment", example = "true")
    private Boolean canEdit;

    @Schema(description = "Whether current user can delete this assignment", example = "true")
    private Boolean canDelete;

    private Boolean canSubmit;

    private String createdByFullName;

    private String createdByAvatar;

    private String createdByEmail;

    private Long announcementId;

    @Schema(description = "List of attachments")
    private List<AttachmentResponse> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        @Schema(description = "Attachment ID", example = "1")
        private Long attachmentId;

        @Schema(description = "File name", example = "homework.pdf")
        private String fileName;

        @Schema(description = "File URL", example = "https://example.com/files/homework.pdf")
        private String fileUrl;

        @Schema(description = "File description", example = "Homework instructions")
        private String description;
    }
}
