package com.vn.backend.dto.response.classroom;

import com.vn.backend.utils.ModelMapperUtils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassMemberSearchResponse {

  private Long memberId;

  private Long userId;

  private String fullName;

  private String username;

  private String code;

  private String email;

  private String phone;

  private String avatarUrl;

  private String memberRole;

  private String memberStatus;

  private LocalDateTime joinedAt;

  public static ClassMemberSearchResponse fromDTO(ClassMemberSearchQueryDTO dto) {
    return ModelMapperUtils.mapTo(dto, ClassMemberSearchResponse.class);
  }
}
