package com.vn.backend.dto.request.approval;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.annotation.ValidEnum;
import com.vn.backend.constants.AppConst;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import com.vn.backend.utils.EnumUtils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalRequestSearchRequest {

    @ValidEnum(enumClass = RequestType.class, fieldName = AppConst.FieldConst.REQUEST_TYPE, message = AppConst.MessageConst.VALUE_OUT_OF_RANGE)
    private String requestType;

    @ValidEnum(enumClass = ApprovalStatus.class, fieldName = AppConst.FieldConst.REQUEST_STATUS, message = AppConst.MessageConst.VALUE_OUT_OF_RANGE)
    private String requestStatus;

    private LocalDateTime createdAtFrom;

    private LocalDateTime createdAtTo;

    public ApprovalRequestSearchRequestDTO toDTO() {
        return ApprovalRequestSearchRequestDTO.builder()
                .requestType(EnumUtils.fromString(RequestType.class, this.requestType))
                .status(EnumUtils.fromString(ApprovalStatus.class, this.requestStatus))
                .createdAtFrom(this.createdAtFrom)
                .createdAtTo(this.createdAtTo)
                .build();
    }
}
