package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.invitation.InvitationSearchRequest;
import com.vn.backend.dto.request.invitation.JoinClassroomByCodeRequest;
import com.vn.backend.dto.request.invitation.RespondInvitationRequest;
import com.vn.backend.dto.request.invitation.SendBulkInvitationRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.invitation.InvitationResponse;
import com.vn.backend.services.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.API + "/invitations")
@Slf4j
@Tag(name = "Invitation Management", description = "APIs for managing classroom invitations")
public class InvitationController extends BaseController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send invitation", description = "Teacher sends invitations to multiple students to join classroom")
    public AppResponse<Void> sendBulkInvitation(
            @Valid @RequestBody SendBulkInvitationRequest request) {

        log.info("Received send bulk invitation request: {} users for classroom {}",
                request.getUserIds().size(), request.getClassroomId());
        invitationService.sendBulkInvitation(request);
        return success(null);
    }

    @PostMapping("/join-by-code")
    @Operation(summary = "Join classroom by code", description = "User joins classroom using class code")
    public AppResponse<Void> joinClassroomByCode(
            @Valid @RequestBody JoinClassroomByCodeRequest request) {
        invitationService.joinClassroomByCode(request);
        return success(null);
    }

    @PostMapping("/respond")
    @Operation(summary = "Respond to invitation", description = "Student accepts or declines classroom invitation")
    public AppResponse<InvitationResponse> respondToInvitation(
            @Valid @RequestBody RespondInvitationRequest request) {
        invitationService.respondToInvitation(request);
        return success(null);
    }

    @PostMapping("/my-invitations")
    @Operation(summary = "Get my invitations with pagination", description = "Get invitations for current user with filtering and pagination")
    public AppResponse<ResponseListData<InvitationResponse>> getMyInvitationsWithPagination(
            @Valid @RequestBody InvitationSearchRequest request) {
        ResponseListData<InvitationResponse> invitations = invitationService.getUserInvitationsWithPagination(request);
        return success(invitations);
    }

}
