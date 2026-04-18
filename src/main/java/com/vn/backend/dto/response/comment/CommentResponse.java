package com.vn.backend.dto.response.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private Long announcementId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User info
    private String userFullName;
    private String userEmail;
    private String userAvatar;
    
    // Permissions
    private Boolean canEdit;
    private Boolean canDelete;
}
