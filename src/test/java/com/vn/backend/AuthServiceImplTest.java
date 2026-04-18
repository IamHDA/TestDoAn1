package com.vn.backend;

import com.vn.backend.configs.jwt.JwtTokenProvider;
import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.LoginRequest;
import com.vn.backend.dto.request.RefreshTokenRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.auth.TokenResponse;
import com.vn.backend.entities.Token;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.TokenRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.CustomUserDetails;
import com.vn.backend.services.impl.AuthServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private MessageUtils messageUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .fullName("Test User")
                .role(Role.STUDENT)
                .isActive(true)
                .isDeleted(false)
                .build();

        customUserDetails = new CustomUserDetails(testUser);
        SecurityContextHolder.setContext(securityContext);
    }

    // ===================== getCurrentUser =====================

    @Test
    @DisplayName("getCurrentUser - thành công khi đã xác thực")
    void getCurrentUser_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsernameAndIsActive("testuser", true))
                .thenReturn(Optional.of(testUser));

        User result = authService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsernameAndIsActive("testuser", true);
    }

    @Test
    @DisplayName("getCurrentUser - ném exception khi chưa xác thực (authentication null)")
    void getCurrentUser_ThrowsException_WhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("getCurrentUser - ném exception khi authentication không authenticated")
    void getCurrentUser_ThrowsException_WhenNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("getCurrentUser - ném exception khi user không tồn tại trong DB")
    void getCurrentUser_ThrowsException_WhenUserNotFoundInDB() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent");
        when(userRepository.findByUsernameAndIsActive("nonexistent", true))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> authService.getCurrentUser())
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    // ===================== login =====================

    @Test
    @DisplayName("login - thành công với thông tin hợp lệ")
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(tokenProvider.generateToken(customUserDetails)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(customUserDetails)).thenReturn("refresh-token");
        when(userRepository.findByUsernameAndIsActive("testuser", true))
                .thenReturn(Optional.of(testUser));

        TokenResponse result = authService.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        verify(tokenRepository).revokeAllUserTokens(testUser);
        verify(tokenRepository).save(any(Token.class));
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("login - ném exception khi user không tìm thấy sau khi xác thực")
    void login_ThrowsException_WhenUserNotFoundAfterAuthentication() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(tokenProvider.generateToken(customUserDetails)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(customUserDetails)).thenReturn("refresh-token");
        when(userRepository.findByUsernameAndIsActive("testuser", true))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.USER_NOT_FOUND)).thenReturn("User not found");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USER_NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ===================== getCurrentUserInfo =====================

    @Test
    @DisplayName("getCurrentUserInfo - thành công khi user hợp lệ")
    void getCurrentUserInfo_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsernameAndIsActive("testuser", true))
                .thenReturn(Optional.of(testUser));

        UserResponse result = authService.getCurrentUserInfo();

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    // ===================== logout =====================

    @Test
    @DisplayName("logout - thành công, revoke tất cả token và clear context")
    void logout_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsernameAndIsActive("testuser", true))
                .thenReturn(Optional.of(testUser));

        authService.logout();

        verify(tokenRepository).revokeAllUserTokens(testUser);
    }

    // ===================== refreshToken =====================

    @Test
    @DisplayName("refreshToken - thành công với refresh token hợp lệ")
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        Token existingToken = Token.builder()
                .accessToken("old-access-token")
                .refreshToken("valid-refresh-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .user(testUser)
                .build();

        when(tokenProvider.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(tokenRepository.findByRefreshTokenAndIsRevokedFalse("valid-refresh-token"))
                .thenReturn(Optional.of(existingToken));
        when(tokenProvider.generateToken(any(CustomUserDetails.class))).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new-refresh-token");

        TokenResponse result = authService.refreshToken(request);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(tokenRepository).revokeRefreshToken("valid-refresh-token");
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    @DisplayName("refreshToken - ném exception khi refresh token không hợp lệ")
    void refreshToken_ThrowsException_WhenTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(tokenProvider.validateRefreshToken("invalid-token")).thenReturn(false);
        when(messageUtils.getMessage(AppConst.MessageConst.INVALID_REFRESH_TOKEN)).thenReturn("Invalid refresh token");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_REFRESH_TOKEN);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("refreshToken - ném exception khi không tìm thấy refresh token trong DB")
    void refreshToken_ThrowsException_WhenTokenNotFoundInDB() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-but-not-found-token");

        when(tokenProvider.validateRefreshToken("valid-but-not-found-token")).thenReturn(true);
        when(tokenRepository.findByRefreshTokenAndIsRevokedFalse("valid-but-not-found-token"))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.INVALID_REFRESH_TOKEN)).thenReturn("Invalid refresh token");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_REFRESH_TOKEN);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("refreshToken - ném exception khi refresh token đã hết hạn")
    void refreshToken_ThrowsException_WhenTokenExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-refresh-token");

        Token expiredToken = Token.builder()
                .accessToken("old-access-token")
                .refreshToken("expired-refresh-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .refreshExpiresAt(LocalDateTime.now().minusDays(1)) // đã hết hạn
                .isRevoked(false)
                .user(testUser)
                .build();

        when(tokenProvider.validateRefreshToken("expired-refresh-token")).thenReturn(true);
        when(tokenRepository.findByRefreshTokenAndIsRevokedFalse("expired-refresh-token"))
                .thenReturn(Optional.of(expiredToken));
        when(messageUtils.getMessage(AppConst.MessageConst.REFRESH_TOKEN_EXPIRED)).thenReturn("Refresh token expired");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.REFRESH_TOKEN_EXPIRED);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}
