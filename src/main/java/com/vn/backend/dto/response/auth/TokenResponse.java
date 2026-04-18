package com.vn.backend.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Simple token response containing only access token and refresh token")
public class TokenResponse {
    
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;
    
    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;

    public TokenResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }
}
