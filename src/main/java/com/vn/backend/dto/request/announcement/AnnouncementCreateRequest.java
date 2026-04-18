package com.vn.backend.dto.request.announcement;

import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.enums.AnnouncementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class AnnouncementCreateRequest {


    @AllowLength(max = 255, message = "Title must not exceed 255 characters", fieldName = "TITLE")
    private String title;

    @NotAllowBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Announcement type is required")
    private AnnouncementType type;

    @Builder.Default
    private Boolean allowComments = true;

    @Valid
    private List<AttachmentRequest> attachments;

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