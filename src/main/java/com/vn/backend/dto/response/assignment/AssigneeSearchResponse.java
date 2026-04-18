package com.vn.backend.dto.response.assignment;

import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssigneeSearchResponse {

  private UserInfoResponse user;
  private boolean isAssigned;

  public static AssigneeSearchResponse fromDTO(AssigneeSearchQueryDTO dto) {
    return AssigneeSearchResponse.builder()
        .isAssigned(dto.isAssigned())
        .user(ModelMapperUtils.mapTo(dto.getUser(), UserInfoResponse.class))
        .build();
  }
}
