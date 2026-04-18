package com.vn.backend.dto.request.user;

import com.vn.backend.enums.Role;
import com.vn.backend.enums.StatusUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User filter criteria")
public class UserFilterRequest {
    
    @Schema(description = "Search term for username, full name, or email", example = "john")
    private String search; // Search by username, fullName, email
    
    @Schema(description = "Filter by role", example = "STUDENT")
    private Role role; // Filter by role
    
    @Schema(description = "Filter by status", example = "ACTIVE")
    private StatusUser status; // Filter by status (ACTIVE/INACTIVE)


}
