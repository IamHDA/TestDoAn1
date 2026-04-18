package com.vn.backend.dto.response.user;

import com.vn.backend.entities.User;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

  private Long id;
  private String username;
  private String avatarUrl;
  private String fullName;

  public static UserInfoResponse fromEntity(User entity) {
    return ModelMapperUtils.mapTo(entity, UserInfoResponse.class);
  }

}
