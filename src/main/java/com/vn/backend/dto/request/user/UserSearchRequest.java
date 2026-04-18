package com.vn.backend.dto.request.user;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "User search request with filters and pagination")
public class UserSearchRequest extends BaseFilterSearchRequest<UserFilterRequest> {
}
