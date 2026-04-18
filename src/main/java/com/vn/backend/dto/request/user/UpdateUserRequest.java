package com.vn.backend.dto.request.user;

import com.vn.backend.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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
public class UpdateUserRequest {
    
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    private String code;
    
    @Email(message = "Email should be valid")
    private String email;

    
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    private String password;
    
    private String avatarUrl;

    private LocalDate dateOfBirth;
    
    private String gender;
    
    private String address;
    
    private Role role;
}
