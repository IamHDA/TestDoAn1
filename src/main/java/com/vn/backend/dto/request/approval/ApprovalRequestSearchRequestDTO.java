package com.vn.backend.dto.request.approval;

import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalRequestSearchRequestDTO {
    private RequestType requestType;
    private ApprovalStatus status;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Long userId;
}
