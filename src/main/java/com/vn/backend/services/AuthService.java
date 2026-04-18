package com.vn.backend.services;

import com.vn.backend.dto.request.LoginRequest;
import com.vn.backend.dto.request.RefreshTokenRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.auth.TokenResponse;
import com.vn.backend.entities.User;

public interface AuthService {
    User getCurrentUser();
    TokenResponse login(LoginRequest loginRequest);
    UserResponse getCurrentUserInfo();
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    void logout();
}

