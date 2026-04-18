package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.LoginRequest;
import com.vn.backend.dto.request.RefreshTokenRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.auth.TokenResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConst.API + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public AppResponse<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Received request to login user");
        TokenResponse response = authService.login(loginRequest);
        log.info("Successfully authenticated user");
        return success(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Get information of currently authenticated user")
    public AppResponse<UserResponse> getCurrentUser() {
        log.info("Received request to get current user info");
        UserResponse response = authService.getCurrentUserInfo();
        log.info("Successfully retrieved current user info");
        return success(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT token", description = "Get new access token using refresh token")
    public AppResponse<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("Received request to refresh token");
        TokenResponse response = authService.refreshToken(refreshTokenRequest);
        log.info("Successfully refreshed token");
        return success(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke all tokens for current user and clear context")
    public AppResponse<Void> logout() {
        log.info("Received request to logout");
        authService.logout();
        log.info("Successfully logged out");
        return success(null);
    }

}