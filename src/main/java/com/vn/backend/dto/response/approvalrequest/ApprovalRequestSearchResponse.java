package com.vn.backend.dto.response.approvalrequest;

import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.entities.ApprovalRequest;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ApprovalRequestSearchResponse {

    private Long id;
    private RequestType requestType;
    private ApprovalStatus status;
    private UserInfoResponse requester;
    private LocalDateTime createdAt;

    public static ApprovalRequestSearchResponse fromEntity(ApprovalRequest entity) {
        return ApprovalRequestSearchResponse.builder()
                .id(entity.getId())
                .requestType(entity.getRequestType())
                .status(entity.getStatus())
                .requester(
                        UserInfoResponse.fromEntity(entity.getRequester())
                )
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
