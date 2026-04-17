package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.user.CreateUserRequest;
import com.vn.backend.dto.request.user.UpdateUserRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.enums.StatusUser;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.UserServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("johndoe")
                .code("STU001")
                .email("john@example.com")
                .password("encodedPassword")
                .fullName("John Doe")
                .phone("0123456789")
                .role(Role.STUDENT)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    // ===================== createUser =====================

    @Test
    @DisplayName("createUser - thành công khi thông tin hợp lệ")
    void createUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .code("STU002")
                .email("newuser@example.com")
                .password("password123")
                .fullName("New User")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByUsernameAndIsActive("newuser", true)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByCodeAndIsActive("STU002", true)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");

        userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword123");
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("createUser - ném exception khi username đã tồn tại")
    void createUser_ThrowsException_WhenUsernameAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("johndoe")
                .code("STU003")
                .email("new@example.com")
                .password("password123")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByUsernameAndIsActive("johndoe", true))
                .thenReturn(Optional.of(existingUser));
        when(messageUtils.getMessage(AppConst.MessageConst.USERNAME_ALREADY_EXISTS)).thenReturn("Username already exists");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USERNAME_ALREADY_EXISTS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - ném exception khi email đã tồn tại")
    void createUser_ThrowsException_WhenEmailAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser2")
                .code("STU004")
                .email("john@example.com")
                .password("password123")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByUsernameAndIsActive("newuser2", true)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(messageUtils.getMessage(AppConst.MessageConst.EMAIL_ALREADY_EXISTS)).thenReturn("Email already exists");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.EMAIL_ALREADY_EXISTS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - ném exception khi code đã tồn tại")
    void createUser_ThrowsException_WhenCodeAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser3")
                .code("STU001")
                .email("newuser3@example.com")
                .password("password123")
                .role(Role.STUDENT)
                .build();

        when(userRepository.findByUsernameAndIsActive("newuser3", true)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser3@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByCodeAndIsActive("STU001", true))
                .thenReturn(Optional.of(existingUser));
        when(messageUtils.getMessage(AppConst.MessageConst.CODE_ALREADY_EXISTS)).thenReturn("Code already exists");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.CODE_ALREADY_EXISTS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(userRepository, never()).save(any());
    }

    // ===================== updateUser =====================

    @Test
    @DisplayName("updateUser - thành công khi cập nhật thông tin hợp lệ")
    void updateUser_Success() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("John Doe Updated")
                .phone("0987654321")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(1L, request);

        verify(userRepository).save(existingUser);
        assertThat(existingUser.getFullName()).isEqualTo("John Doe Updated");
        assertThat(existingUser.getPhone()).isEqualTo("0987654321");
    }

    @Test
    @DisplayName("updateUser - ném exception khi user không tồn tại")
    void updateUser_ThrowsException_WhenUserNotFound() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(eq(AppConst.MessageConst.USER_NOT_FOUND), any())).thenReturn("User not found");

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USER_NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("updateUser - ném exception khi đổi username thành username đã tồn tại")
    void updateUser_ThrowsException_WhenNewUsernameAlreadyTaken() {
        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .email("another@example.com")
                .build();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("anotheruser")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameAndIsActive("anotheruser", true))
                .thenReturn(Optional.of(anotherUser));
        when(messageUtils.getMessage(AppConst.MessageConst.USERNAME_ALREADY_EXISTS)).thenReturn("Username already exists");

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USERNAME_ALREADY_EXISTS);
                });
    }

    @Test
    @DisplayName("updateUser - thành công khi giữ nguyên username cũ")
    void updateUser_Success_WhenSameUsername() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("johndoe") // same username
                .fullName("John Doe Updated")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUser(1L, request);

        // username không thay đổi nên không cần kiểm tra trùng
        verify(userRepository, never()).findByUsernameAndIsActive(anyString(), eq(true));
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUser - cập nhật password được encode")
    void updateUser_EncodesPasswordWhenProvided() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .password("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        userService.updateUser(1L, request);

        assertThat(existingUser.getPassword()).isEqualTo("newEncodedPassword");
        verify(passwordEncoder).encode("newPassword123");
    }

    // ===================== updateUserStatus =====================

    @Test
    @DisplayName("updateUserStatus - thành công khi vô hiệu hóa user")
    void updateUserStatus_Success_Deactivate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUserStatus(1L, StatusUser.INACTIVE);

        assertThat(existingUser.getIsActive()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUserStatus - thành công khi kích hoạt lại user")
    void updateUserStatus_Success_Activate() {
        existingUser.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.updateUserStatus(1L, StatusUser.ACTIVE);

        assertThat(existingUser.getIsActive()).isTrue();
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("updateUserStatus - ném exception khi user không tồn tại")
    void updateUserStatus_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(eq(AppConst.MessageConst.USER_NOT_FOUND), any())).thenReturn("User not found");

        assertThatThrownBy(() -> userService.updateUserStatus(99L, StatusUser.INACTIVE))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USER_NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ===================== getUserById =====================

    @Test
    @DisplayName("getUserById - thành công khi user tồn tại")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("getUserById - ném exception khi user không tồn tại")
    void getUserById_ThrowsException_WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(eq(AppConst.MessageConst.USER_NOT_FOUND), any())).thenReturn("User not found");

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.USER_NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
