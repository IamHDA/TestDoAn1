package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest;
import com.vn.backend.dto.request.approval.ApproveRejectRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestDetailResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestSearchResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.services.ApprovalRequestService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping(AppConst.API + "/approval-requests")
@RestController
public class ApprovalRequestController extends BaseController {

    private final ApprovalRequestService approvalRequestService;

    public ApprovalRequestController(ApprovalRequestService approvalRequestService) {
        this.approvalRequestService = approvalRequestService;
    }

    @GetMapping("/{id}")
    public AppResponse<ApprovalRequestDetailResponse> getDetail(@PathVariable("id") Long id) {
        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(id);
        return success(response);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<ApprovalRequestSearchResponse>> search(
            @RequestBody @Valid BaseFilterSearchRequest<ApprovalRequestSearchRequest> request
    ) {
        ResponseListData<ApprovalRequestSearchResponse> response = approvalRequestService.searchApprovalRequest(request);
        return successListData(response);
    }

    @PostMapping("/{id}/approve")
    public AppResponse<Void> approve(@PathVariable("id") Long id) {
        approvalRequestService.approveRequest(id);
        return success(null);
    }

    @PostMapping("/{id}/reject")
    public AppResponse<Void> reject(
            @PathVariable("id") Long id,
            @RequestBody @Valid ApproveRejectRequest request
    ) {
        approvalRequestService.rejectRequest(id, request);
        return success(null);
    }
}

