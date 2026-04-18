package com.vn.backend.dto.response.classroom;

import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassMemberSearchQueryDTO {

  private Long memberId;

  private Long userId;

  private String fullName;

  private String username;

  private String code;

  private String email;

  private String phone;

  private String avatarUrl;

  private ClassMemberRole memberRole;

  private ClassMemberStatus memberStatus;

  private LocalDateTime joinedAt;
}
