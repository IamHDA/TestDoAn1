package com.vn.backend.services;


import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest;
import com.vn.backend.dto.request.approval.ApproveRejectRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestDetailResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestSearchResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.enums.RequestType;

import java.util.List;

public interface ApprovalRequestService {

    void createRequest(RequestType requestType, String requestDescription, Long requesterId, List<Long> entityIds);

    ApprovalRequestDetailResponse getApprovalRequestDetail(Long id);
    ResponseListData<ApprovalRequestSearchResponse> searchApprovalRequest(BaseFilterSearchRequest<ApprovalRequestSearchRequest> request);
    
    void approveRequest(Long requestId);
    void rejectRequest(Long requestId, ApproveRejectRequest request);
}
