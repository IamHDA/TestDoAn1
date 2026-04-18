package com.vn.backend.dto.request.invitation;

import com.vn.backend.enums.ClassroomInvitationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondInvitationRequest {

    @NotNull(message = "Invitation ID is required")
    private Long invitationId;

    @NotNull(message = "Response status is required")
    private ClassroomInvitationStatus responseStatus;
}
