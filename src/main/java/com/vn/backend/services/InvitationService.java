package com.vn.backend.services;

import com.vn.backend.dto.request.invitation.InvitationSearchRequest;
import com.vn.backend.dto.request.invitation.JoinClassroomByCodeRequest;
import com.vn.backend.dto.request.invitation.RespondInvitationRequest;
import com.vn.backend.dto.request.invitation.SendBulkInvitationRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.invitation.InvitationResponse;

import java.util.List;

public interface InvitationService {


    /**
     * Gửi lời mời hàng loạt của Teacher cho nhiều Student
     * @param request Thông tin lời mời hàng loạt
     */
    void sendBulkInvitation(SendBulkInvitationRequest request);


    void respondToInvitation(RespondInvitationRequest request);

    /**
     * User tham gia classroom bằng class code
     * @param request Thông tin class code
     */
    void joinClassroomByCode(JoinClassroomByCodeRequest request);


    /**
     * Lấy danh sách lời mời của một user với filter và pagination
     * @param request Thông tin filter và pagination
     * @return Danh sách lời mời có phân trang
     */
    ResponseListData<InvitationResponse> getUserInvitationsWithPagination(InvitationSearchRequest request);


}
