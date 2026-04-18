package com.vn.backend.services.impl;

import com.vn.backend.configs.jwt.JwtTokenProvider;
import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.LoginRequest;
import com.vn.backend.dto.request.RefreshTokenRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.auth.TokenResponse;
import com.vn.backend.entities.Token;
import com.vn.backend.entities.User;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.TokenRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.CustomUserDetails;
import com.vn.backend.utils.MessageUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
public class AuthServiceImpl extends BaseService implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenPovider;

    public AuthServiceImpl(MessageUtils messageUtils, UserRepository userRepository, TokenRepository tokenRepository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtTokenProvider tokenPovider) {
        super(messageUtils);
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.tokenPovider = tokenPovider;
    }

    // Lấy User hiện tại từ context
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED, messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED),HttpStatus.UNAUTHORIZED);
        String username = authentication.getName();
        return userRepository.findByUsernameAndIsActive(username, true).orElseThrow(() -> new AppException(AppConst.MessageConst.UNAUTHORIZED, messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED),HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    @Override
    public TokenResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // Tạo tokens
        String jwt = tokenPovider.generateToken(customUserDetails);
        String refreshToken = tokenPovider.generateRefreshToken(customUserDetails);
        
        // Lấy user từ database
        User user = userRepository.findByUsernameAndIsActive(loginRequest.getUsername(), true)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.USER_NOT_FOUND, messageUtils.getMessage(AppConst.MessageConst.USER_NOT_FOUND),HttpStatus.BAD_REQUEST));
        
        // Revoke tất cả token cũ của user
        tokenRepository.revokeAllUserTokens(user);
        
        // Tính toán thời gian hết hạn
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiry = now.plusSeconds(360000); // 100 giờ
        LocalDateTime refreshTokenExpiry = now.plusSeconds(604800); // 7 ngày
        
        // Lưu token vào database
        Token tokenEntity = Token.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .expiresAt(accessTokenExpiry)
                .refreshExpiresAt(refreshTokenExpiry)
                .isRevoked(false)
                .user(user)
                .build();
        
        tokenRepository.save(tokenEntity);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        return new TokenResponse(jwt, refreshToken);
    }

    @Override
    public UserResponse getCurrentUserInfo() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED, messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED),HttpStatus.UNAUTHORIZED);
        }
        return UserResponse.fromUser(currentUser);
    }

    @Override
    @Transactional
    public void logout() {
        User currentUser = getCurrentUser();
        tokenRepository.revokeAllUserTokens(currentUser);
        SecurityContextHolder.clearContext();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        
        // Validate refresh token
        if (!tokenPovider.validateRefreshToken(refreshToken)) {
            throw new AppException(AppConst.MessageConst.INVALID_REFRESH_TOKEN, messageUtils.getMessage(AppConst.MessageConst.INVALID_REFRESH_TOKEN),HttpStatus.UNAUTHORIZED);
        }
        
        // Tìm token trong database
        Token existingToken = tokenRepository.findByRefreshTokenAndIsRevokedFalse(refreshToken)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.INVALID_REFRESH_TOKEN, messageUtils.getMessage(AppConst.MessageConst.INVALID_REFRESH_TOKEN),HttpStatus.UNAUTHORIZED));
        
        // Kiểm tra token có hết hạn chưa
        if (existingToken.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(AppConst.MessageConst.REFRESH_TOKEN_EXPIRED, messageUtils.getMessage(AppConst.MessageConst.REFRESH_TOKEN_EXPIRED),HttpStatus.UNAUTHORIZED);
        }
        
        User user = existingToken.getUser();
        
        // Revoke token cũ
        tokenRepository.revokeRefreshToken(refreshToken);
        
        // Tạo CustomUserDetails từ User entity
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        
        // Tạo tokens mới
        String newAccessToken = tokenPovider.generateToken(customUserDetails);
        String newRefreshToken = tokenPovider.generateRefreshToken(customUserDetails);
        
        // Tính toán thời gian hết hạn
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiry = now.plusSeconds(360000); // 100 giờ
        LocalDateTime refreshTokenExpiry = now.plusSeconds(604800); // 7 ngày
        
        // Lưu token mới vào database
        Token newTokenEntity = Token.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresAt(accessTokenExpiry)
                .refreshExpiresAt(refreshTokenExpiry)
                .isRevoked(false)
                .user(user)
                .build();
        
        tokenRepository.save(newTokenEntity);
        
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

}
