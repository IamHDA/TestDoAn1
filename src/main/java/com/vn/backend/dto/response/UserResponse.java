package com.vn.backend.dto.response;

import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String code;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean isDeleted;
    
    private LocalDate dateOfBirth;
    
    private String gender;
    private String address;
    
    private LocalDateTime lastLogin;
    
    private Role role;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .code(user.getCode())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .address(user.getAddress())
                .lastLogin(user.getLastLogin())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
