package com.vn.backend.dto.request.invitation;

import com.vn.backend.enums.ClassroomInvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitationFilterRequest {

    @Schema(description = "Filter by invitation status", example = "PENDING")
    private ClassroomInvitationStatus status;
}
