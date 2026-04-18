package com.vn.backend.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchForInviteResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
}
