package com.vn.backend.dto.request.assignment;

import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class AssignmentCreateRequest {


    private String title;

    private String description;

    private Long maxScore;

    private LocalDateTime dueDate;

    private Boolean submissionClosed;

    private String content;

    @Builder.Default
    private Boolean allowComments = true;

    @Valid
    private List<AnnouncementCreateRequest.AttachmentRequest> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentRequest {
        @NotAllowBlank(message = "File name is required")
        private String fileName;

        @NotAllowBlank(message = "File URL is required")
        private String fileUrl;

        private String description;
    }
}
