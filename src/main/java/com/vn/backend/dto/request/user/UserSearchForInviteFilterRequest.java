package com.vn.backend.dto.request.user;

import com.vn.backend.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter criteria for searching users to invite")
public class UserSearchForInviteFilterRequest {
    
    @Schema(description = "Search term for username, full name, or email", example = "john")
    private String search; // Search by username, fullName, email
    
    @Schema(description = "Filter by role (only STUDENT or TEACHER allowed)", example = "STUDENT", allowableValues = {"STUDENT", "TEACHER"})
    private Role role; // Filter by role (chỉ STUDENT và TEACHER)
}
