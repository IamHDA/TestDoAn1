package com.vn.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request for refreshing JWT token")
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
}
