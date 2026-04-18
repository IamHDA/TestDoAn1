package com.vn.backend.dto.request.invitation;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitationSearchRequest extends BaseFilterSearchRequest<InvitationFilterRequest> {

    @Schema(description = "Filter criteria for invitations")
    @Override
    public InvitationFilterRequest getFilters() {
        return super.getFilters();
    }

    @Schema(description = "Pagination and sorting criteria")
    @Override
    public com.vn.backend.dto.request.common.SearchRequest getPagination() {
        return super.getPagination();
    }
}
