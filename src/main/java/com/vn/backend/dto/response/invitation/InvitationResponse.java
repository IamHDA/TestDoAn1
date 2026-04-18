package com.vn.backend.dto.response.invitation;

import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassroomInvitationStatus;
import com.vn.backend.enums.ClassroomInvitationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private Long invitationId;
    private Long classroomId;
    private String className;
    private String classCode;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private ClassroomInvitationType invitationType;
    private ClassMemberRole memberRole;
    private Long invitedBy;
    private String inviterFullName;
    private String inviterAvatar;
    private ClassroomInvitationStatus invitationStatus;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
