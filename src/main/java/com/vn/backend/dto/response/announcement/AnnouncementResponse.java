package com.vn.backend.dto.response.announcement;

import com.vn.backend.enums.AnnouncementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private Long announcementId;
    private String title;
    private String content;
    private AnnouncementType type;
    private Boolean allowComments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private String createdByFullName;
    private String avatarUrl;
    private Boolean canEdit;
    private Boolean canDelete;
    private Long objectId;
    private List<AttachmentResponse> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        private Long attachmentId;
        private String fileName;
        private String fileUrl;
        private String description;
    }
}
