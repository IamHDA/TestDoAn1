package com.vn.backend.dto.request.announcement;

import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementUpdateRequest {

    @AllowLength(max = 255, message = "Title must not exceed 255 characters", fieldName = "TITLE")
    private String title;

    @NotAllowBlank(message = "Content is required")
    private String content;

    private Boolean allowComments;

    @Valid
    private List<AttachmentUpdateRequest> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentUpdateRequest {
        private Long attachmentId;

        private String fileName;

        private String fileUrl;

        private String description;
    }
}
