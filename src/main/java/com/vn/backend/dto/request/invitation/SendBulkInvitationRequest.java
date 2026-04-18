package com.vn.backend.dto.request.invitation;

import com.vn.backend.enums.ClassMemberRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendBulkInvitationRequest {

    @NotNull(message = "Classroom ID is required")
    private Long classroomId;

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;

    @NotNull(message = "Invitation role is required")
    private ClassMemberRole classMemberRole;
}
