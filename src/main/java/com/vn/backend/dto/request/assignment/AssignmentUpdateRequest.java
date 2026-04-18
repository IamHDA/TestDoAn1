package com.vn.backend.dto.request.assignment;

import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.enums.AnnouncementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentUpdateRequest {
    private Boolean submissionClosed;
    private LocalDateTime dueDate;
    private Long maxScore;

    private String title;

    private String content;

    private Boolean allowComments = true;

    @Valid
    private List<AttachmentRequest> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentRequest {
        private Long attachmentId;

        @NotAllowBlank(message = "File name is required")
        private String fileName;

        @NotAllowBlank(message = "File URL is required")
        private String fileUrl;

        private String description;
    }
}
