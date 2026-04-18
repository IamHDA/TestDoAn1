package com.vn.backend.dto.response.approvalrequest;

import com.vn.backend.dto.response.topic.TopicDetailResponse;
import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.entities.ApprovalRequest;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApprovalRequestDetailResponse {

    private Long id;
    private RequestType requestType;
    private ApprovalStatus status;
    private String description;
    private String rejectReason;
    private UserInfoResponse requester;
    private UserInfoResponse reviewer;
    private LocalDateTime createdAt;
    private List<ApprovalRequestItemResponse> requestItems;
    private List<TopicDetailResponse> currentTopics;

    public static ApprovalRequestDetailResponse fromEntity(ApprovalRequest entity) {
        return ApprovalRequestDetailResponse.builder()
                .id(entity.getId())
                .requestType(entity.getRequestType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .rejectReason(entity.getRejectReason())
                .createdAt(entity.getCreatedAt())
                .requester(UserInfoResponse.fromEntity(entity.getRequester()))
                .reviewer(entity.getReviewerId() != null && entity.getReviewer() != null 
                        ? UserInfoResponse.fromEntity(entity.getReviewer()) : null)
                .build();
    }
}
