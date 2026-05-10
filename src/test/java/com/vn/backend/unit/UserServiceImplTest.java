package com.vn.backend.unit;


import org.junit.jupiter.api.DisplayName;

import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.user.*;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.user.UserSearchForInviteResponse;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.enums.StatusUser;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.UserServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Lớp kiểm thử cho UserServiceImpl, quản lý các unit test cho chức năng người
 * dùng.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    private UserServiceImpl service;

    private final Map<Long, User> userStore = new HashMap<>();
    private User savedUser;

    /**
     * Thiết lập môi trường trước mỗi bài kiểm thử.
     */
    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new UserServiceImpl(
                messageUtils,
                userRepository,
                passwordEncoder,
                authService);

        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            savedUser = invocation.getArgument(0);
            if (savedUser.getId() == null) {
                savedUser.setId((long) (userStore.size() + 1));
            }
            userStore.put(savedUser.getId(), savedUser);
            return savedUser;
        });

        when(userRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.ofNullable(userStore.get(id));
        });

        when(userRepository.findByUsernameAndIsActive(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String username = invocation.getArgument(0);
            Boolean active = invocation.getArgument(1);

            return userStore.values()
                    .stream()
                    .filter(user -> username.equals(user.getUsername()))
                    .filter(user -> active.equals(user.getIsActive()))
                    .findFirst();
        });

        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);

            return userStore.values()
                    .stream()
                    .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                    .findFirst();
        });

        when(userRepository.findByCodeAndIsActive(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            Boolean active = invocation.getArgument(1);

            return userStore.values()
                    .stream()
                    .filter(user -> code.equals(user.getCode()))
                    .filter(user -> active.equals(user.getIsActive()))
                    .findFirst();
        });
    }

    private User user(Long id, String username, String email, String code, Role role) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .code(code)
                .password("encoded-password")
                .fullName("Nguyen Van A")
                .phone("0123456789")
                .avatarUrl("avatar.png")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender("MALE")
                .address("Ha Noi")
                .role(role)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    private CreateUserRequest createRequest() {
        CreateUserRequest request = new CreateUserRequest();

        request.setUsername("student01");
        request.setCode("SV001");
        request.setEmail("student01@example.com");
        request.setPassword("123456");
        request.setFullName("Nguyen Van A");
        request.setPhone("0123456789");
        request.setAvatarUrl("avatar.png");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));
        request.setGender("MALE");
        request.setAddress("Ha Noi");
        request.setRole(Role.STUDENT);

        return request;
    }

    private UpdateUserRequest updateRequest() {
        UpdateUserRequest request = new UpdateUserRequest();

        request.setUsername("student02");
        request.setCode("SV002");
        request.setEmail("student02@example.com");
        request.setPassword("new-password");
        request.setFullName("Tran Van B");
        request.setPhone("0987654321");
        request.setAvatarUrl("new-avatar.png");
        request.setDateOfBirth(LocalDate.of(2001, 2, 2));
        request.setGender("FEMALE");
        request.setAddress("Da Nang");
        request.setRole(Role.TEACHER);

        return request;
    }

    private SearchRequest pagination() {
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        return pagination;
    }

    private UserSearchRequest userSearchRequest(UserFilterRequest filters) {
        UserSearchRequest request = new UserSearchRequest();
        request.setFilters(filters);
        request.setPagination(pagination());
        return request;
    }

    private UserSearchForInviteRequest inviteSearchRequest(String search, Role role) {
        UserSearchForInviteFilterRequest filters = new UserSearchForInviteFilterRequest();
        filters.setSearch(search);
        filters.setRole(role);

        UserSearchForInviteRequest request = new UserSearchForInviteRequest();
        request.setFilters(filters);
        request.setPagination(pagination());

        return request;
    }

    private MultipartFile excelFile(List<List<String>> rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("STT");
        header.createCell(1).setCellValue("Full Name");
        header.createCell(2).setCellValue("Code");
        header.createCell(3).setCellValue("Email");
        header.createCell(4).setCellValue("Phone");
        header.createCell(5).setCellValue("Date of Birth (dd/MM/yyyy)");
        header.createCell(6).setCellValue("Gender");
        header.createCell(7).setCellValue("Address");
        header.createCell(8).setCellValue("Role");

        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            List<String> values = rows.get(i);
            for (int j = 0; j < values.size(); j++) {
                row.createCell(j).setCellValue(values.get(j));
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray());
    }

    /**
     * Các bài kiểm thử cho chức năng tạo người dùng.
     */
    @Nested
    class CreateUserTests {

        @Test
        @DisplayName("HT_TK_01 - Đảm bảo luồng tạo mới tài khoản thủ công với đầy đủ thông tin định danh và gán vai trò.")
        void createUser_Success() {
            // Given & When: Thực hiện tạo người dùng mới từ request mẫu
            service.createUser(createRequest());

            // Then: Kiểm tra thông tin người dùng được lưu trữ chính xác
            assertNotNull(savedUser);
            assertEquals("student01", savedUser.getUsername());
            assertEquals("SV001", savedUser.getCode());
            assertEquals("student01@example.com", savedUser.getEmail());
            // Mật khẩu phải được mã hóa trước khi lưu
            assertEquals("encoded-123456", savedUser.getPassword());
            assertEquals("Nguyen Van A", savedUser.getFullName());
            assertEquals(Role.STUDENT, savedUser.getRole());
            assertTrue(savedUser.getIsActive());
            assertFalse(savedUser.getIsDeleted());

            // Đảm bảo hàm save của repository đã được gọi
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_02 - Đảm bảo hệ thống chặn tạo/cập nhật tài khoản khi username đã tồn tại.")
        void createUser_Fail_ThrowsWhenUsernameExists() {
            userStore.put(1L, user(1L, "student01", "old@example.com", "OLD001", Role.STUDENT));

            assertThrows(AppException.class, () -> service.createUser(createRequest()));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_03 - Đảm bảo hệ thống chặn tạo/cập nhật tài khoản khi email đã tồn tại.")
        void createUser_Fail_ThrowsWhenEmailExists() {
            userStore.put(1L, user(1L, "old", "student01@example.com", "OLD001", Role.STUDENT));

            assertThrows(AppException.class, () -> service.createUser(createRequest()));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_04 - Đảm bảo hệ thống chặn tạo tài khoản khi mã định danh đã tồn tại.")
        void createUser_Fail_ThrowsWhenCodeExists() {
            userStore.put(1L, user(1L, "old", "old@example.com", "SV001", Role.STUDENT));

            assertThrows(AppException.class, () -> service.createUser(createRequest()));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_05 - Đảm bảo validate độ dài họ tên người dùng.")
        void createUser_Fail_ThrowsWhenFullNameTooLong() {
            CreateUserRequest request = createRequest();
            request.setFullName("A".repeat(51));

            assertThrows(AppException.class, () -> service.createUser(request));
        }

        @Test
        @DisplayName("HT_TK_06 - Đảm bảo validate độ dài email người dùng.")
        void createUser_Fail_ThrowsWhenEmailTooLong() {
            CreateUserRequest request = createRequest();
            request.setEmail("a".repeat(321));

            assertThrows(AppException.class, () -> service.createUser(request));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật thông tin người dùng.
     */
    @Nested
    class UpdateUserTests {

        @Test
        @DisplayName("HT_TK_07 - Đảm bảo cập nhật tài khoản thành công khi User tồn tại và dữ liệu hợp lệ.")
        void updateUser_Success() {
            User existing = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            userStore.put(USER_ID, existing);

            service.updateUser(USER_ID, updateRequest());

            assertNotNull(savedUser);
            assertEquals("student02", savedUser.getUsername());
            assertEquals("SV002", savedUser.getCode());
            assertEquals("student02@example.com", savedUser.getEmail());
            assertEquals("encoded-new-password", savedUser.getPassword());
            assertEquals("Tran Van B", savedUser.getFullName());
            assertEquals("0987654321", savedUser.getPhone());
            assertEquals("new-avatar.png", savedUser.getAvatarUrl());
            assertEquals(Role.TEACHER, savedUser.getRole());

            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName("HT_TK_08 - Đảm bảo cập nhật thông tin khác không gây lỗi trùng khi username/email giữ nguyên.")
        void updateUser_Success_DoesNotChangeUsernameAndEmailWhenSameValue() {
            User existing = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            userStore.put(USER_ID, existing);

            UpdateUserRequest request = new UpdateUserRequest();
            request.setUsername("student01");
            request.setEmail("student01@example.com");
            request.setFullName("New Name");

            service.updateUser(USER_ID, request);

            assertEquals("student01", savedUser.getUsername());
            assertEquals("student01@example.com", savedUser.getEmail());
            assertEquals("New Name", savedUser.getFullName());
        }

        @Test
        @DisplayName("HT_TK_09 - Đảm bảo xử lý lỗi khi User cần cập nhật/trạng thái/tra cứu không tồn tại.")
        void updateUser_Fail_ThrowsWhenUserMissing() {
            assertThrows(AppException.class, () -> service.updateUser(99L, updateRequest()));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_10 - Đảm bảo hệ thống chặn tạo/cập nhật tài khoản khi username đã tồn tại.")
        void updateUser_Fail_ThrowsWhenUsernameExists() {
            User current = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            User other = user(2L, "student02", "other@example.com", "SV002", Role.STUDENT);

            userStore.put(USER_ID, current);
            userStore.put(2L, other);

            UpdateUserRequest request = updateRequest();
            request.setUsername("student02");

            assertThrows(AppException.class, () -> service.updateUser(USER_ID, request));
        }

        @Test
        @DisplayName("HT_TK_11 - Đảm bảo hệ thống chặn tạo/cập nhật tài khoản khi email đã tồn tại.")
        void updateUser_Fail_ThrowsWhenEmailExists() {
            User current = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            User other = user(2L, "student02", "student02@example.com", "SV002", Role.STUDENT);

            userStore.put(USER_ID, current);
            userStore.put(2L, other);

            UpdateUserRequest request = updateRequest();
            request.setEmail("student02@example.com");

            assertThrows(AppException.class, () -> service.updateUser(USER_ID, request));
        }

        @Test
        @DisplayName("HT_TK_12 - Đảm bảo validate độ dài họ tên người dùng.")
        void updateUser_Fail_ThrowsWhenFullNameTooLong() {
            userStore.put(USER_ID, user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT));

            UpdateUserRequest request = updateRequest();
            request.setFullName("A".repeat(51));

            assertThrows(AppException.class, () -> service.updateUser(USER_ID, request));
        }

        @Test
        @DisplayName("HT_TK_13 - Đảm bảo validate độ dài email người dùng.")
        void updateUser_Fail_ThrowsWhenEmailTooLong() {
            userStore.put(USER_ID, user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT));

            UpdateUserRequest request = updateRequest();
            request.setEmail("a".repeat(321));

            assertThrows(AppException.class, () -> service.updateUser(USER_ID, request));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng cập nhật trạng thái người dùng.
     */
    @Nested
    class UpdateUserStatusTests {

        @Test
        @DisplayName("HT_TK_14 - Đảm bảo Admin có thể khóa/vô hiệu hóa tài khoản.")
        void updateUserStatus_Success_DeactivatesUser() {
            User existing = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            userStore.put(USER_ID, existing);

            service.updateUserStatus(USER_ID, StatusUser.INACTIVE);

            assertNotNull(savedUser);
            assertFalse(savedUser.getIsActive());
            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName("HT_TK_15 - Đảm bảo Admin có thể mở khóa/kích hoạt lại tài khoản.")
        void updateUserStatus_Success_ActivatesUser() {
            // Given: Người dùng hiện đang ở trạng thái INACTIVE (isActive = false)
            User existing = user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT);
            existing.setIsActive(false);
            userStore.put(USER_ID, existing);

            // When: Thực hiện cập nhật trạng thái người dùng thành ACTIVE
            service.updateUserStatus(USER_ID, StatusUser.ACTIVE);

            // Then: Trạng thái isActive của người dùng phải chuyển thành true
            assertTrue(savedUser.getIsActive());
        }

        @Test
        @DisplayName("HT_TK_16 - Đảm bảo xử lý lỗi khi User cần cập nhật/trạng thái/tra cứu không tồn tại.")
        void updateUserStatus_Fail_ThrowsWhenUserMissing() {
            assertThrows(AppException.class, () -> service.updateUserStatus(99L, StatusUser.ACTIVE));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy danh sách người dùng.
     */
    @Nested
    class GetUsersTests {

        @Test
        @DisplayName("HT_TK_17 - Đảm bảo lấy danh sách user dùng truy vấn mặc định khi không có filter.")
        void getUsers_Success_UsesFindUsersWhenNoFilters() {
            UserSearchRequest request = userSearchRequest(null);

            when(userRepository.findUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            user(1L, "student01", "student01@example.com", "SV001", Role.STUDENT))));

            ResponseListData<UserResponse> result = service.getUsers(request);

            assertNotNull(result);
            verify(userRepository).findUsers(any(Pageable.class));
            verify(userRepository, never()).findByFilters(any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("HT_TK_18 - Đảm bảo tìm kiếm user theo search, role và status.")
        void getUsers_Success_UsesFindByFiltersWhenSearchProvided() {
            UserFilterRequest filters = new UserFilterRequest();
            filters.setSearch("student");
            filters.setRole(Role.STUDENT);
            filters.setStatus(StatusUser.ACTIVE);

            when(userRepository.findByFilters(
                    any(),
                    eq(Role.STUDENT),
                    eq(true),
                    any(Pageable.class))).thenReturn(new PageImpl<>(
                            List.of(
                                    user(1L, "student01", "student01@example.com", "SV001", Role.STUDENT))));

            ResponseListData<UserResponse> result = service.getUsers(userSearchRequest(filters));

            assertNotNull(result);
            verify(userRepository).findByFilters(any(), eq(Role.STUDENT), eq(true), any(Pageable.class));
            verify(userRepository, never()).findUsers(any(Pageable.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng tải template nhập người dùng.
     */
    @Nested
    class DownloadUserImportTemplateTests {

        @Test
        @DisplayName("HT_TK_19 - Đảm bảo tải template Excel import người dùng.")
        void downloadUserImportTemplate_Success_ReturnsExcelResource() {
            ByteArrayResource resource = service.downloadUserImportTemplate();

            assertNotNull(resource);
            assertTrue(resource.contentLength() > 0);
        }
    }

    /**
     * Các bài kiểm thử cho chức năng nhập người dùng từ Excel.
     */
    @Nested
    class ImportUsersFromExcelTests {

        @Test
        @DisplayName("HT_TK_20 - Đảm bảo import thành công một dòng user hợp lệ từ Excel.")
        void importUsersFromExcel_Success_ImportsValidRows() throws Exception {
            // Given: Một file Excel giả lập chứa thông tin 1 sinh viên hợp lệ
            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "STUDENT")));

            // When: Thực hiện import người dùng từ file Excel
            service.importUsersFromExcel(file);

            // Then: Kiểm tra thông tin sinh viên được import vào hệ thống
            assertNotNull(savedUser);
            // Username mặc định lấy theo Code nếu không có username riêng
            assertEquals("SV001", savedUser.getUsername());
            assertEquals("SV001", savedUser.getCode());
            assertEquals("a@example.com", savedUser.getEmail());
            assertEquals("Nguyen Van A", savedUser.getFullName());
            assertEquals(LocalDate.of(2000, 1, 1), savedUser.getDateOfBirth());
            assertEquals(Role.STUDENT, savedUser.getRole());
            // Mật khẩu mặc định thường được tạo từ ngày sinh (ddMMyyyy) và được mã hóa
            assertEquals("encoded-01012000", savedUser.getPassword());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_21 - Đảm bảo hệ thống tự sinh mã khi file Excel không có code.")
        void importUsersFromExcel_Success_GeneratesCodeWhenCodeBlank() throws Exception {
            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "", "a@example.com", "0123456789", "2000-01-01", "MALE", "Ha Noi",
                            "STUDENT")));

            when(userRepository.findCodesByPrefix(anyString())).thenReturn(List.of("anguyen1", "anguyen2"));

            service.importUsersFromExcel(file);

            assertNotNull(savedUser);
            assertNotNull(savedUser.getCode());
            assertEquals("a@example.com", savedUser.getEmail());
            assertEquals("encoded-01012000", savedUser.getPassword());
        }

        @Test
        @DisplayName("HT_TK_22 - Đảm bảo phát hiện email bị trùng ngay trong file import.")
        void importUsersFromExcel_Fail_ThrowsWhenEmailDuplicatedInFile() throws Exception {
            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "STUDENT"),
                    List.of("2", "Tran Van B", "SV002", "a@example.com", "0987654321", "02/02/2000", "FEMALE",
                            "Da Nang", "STUDENT")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_23 - Đảm bảo phát hiện code bị trùng ngay trong file import.")
        void importUsersFromExcel_Fail_ThrowsWhenCodeDuplicatedInFile() throws Exception {
            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "STUDENT"),
                    List.of("2", "Tran Van B", "SV001", "b@example.com", "0987654321", "02/02/2000", "FEMALE",
                            "Da Nang", "STUDENT")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_24 - Đảm bảo import từ chối role không hợp lệ.")
        void importUsersFromExcel_Fail_ThrowsWhenRoleInvalid() throws Exception {
            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "INVALID")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_25 - Đảm bảo import từ chối dòng thiếu trường bắt buộc.")
        void importUsersFromExcel_Fail_ThrowsWhenRequiredFieldsMissing() throws Exception {
            MultipartFile file = excelFile(List.of(
                    List.of("1", "", "SV001", "", "0123456789", "01/01/2000", "MALE", "Ha Noi", "")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_26 - Đảm bảo hệ thống chặn tạo/cập nhật tài khoản khi email đã tồn tại.")
        void importUsersFromExcel_Fail_ThrowsWhenEmailExistsInSystem() throws Exception {
            userStore.put(1L, user(1L, "old", "a@example.com", "OLD001", Role.STUDENT));

            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "STUDENT")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("HT_TK_27 - Đảm bảo hệ thống chặn tạo tài khoản khi mã định danh đã tồn tại.")
        void importUsersFromExcel_Fail_ThrowsWhenCodeExistsInSystem() throws Exception {
            userStore.put(1L, user(1L, "old", "old@example.com", "SV001", Role.STUDENT));

            MultipartFile file = excelFile(List.of(
                    List.of("1", "Nguyen Van A", "SV001", "a@example.com", "0123456789", "01/01/2000", "MALE", "Ha Noi",
                            "STUDENT")));

            assertThrows(AppException.class, () -> service.importUsersFromExcel(file));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng lấy thông tin người dùng theo ID.
     */
    @Nested
    class GetUserByIdTests {

        @Test
        @DisplayName("HT_TK_28 - Đảm bảo xem chi tiết user theo ID thành công.")
        void getUserById_Success() {
            // Given: Người dùng tồn tại trong hệ thống
            userStore.put(USER_ID, user(USER_ID, "student01", "student01@example.com", "SV001", Role.STUDENT));

            // When: Lấy thông tin người dùng theo ID
            UserResponse response = service.getUserById(USER_ID);

            // Then: Kết quả trả về phải đúng thông tin của người dùng đó
            assertNotNull(response);
            assertEquals(USER_ID, response.getId());
            assertEquals("student01", response.getUsername());
        }

        @Test
        @DisplayName("HT_TK_29 - Đảm bảo xử lý lỗi khi User cần cập nhật/trạng thái/tra cứu không tồn tại.")
        void getUserById_Fail_ThrowsWhenUserMissing() {
            assertThrows(AppException.class, () -> service.getUserById(99L));
        }
    }

    /**
     * Các bài kiểm thử cho chức năng tìm kiếm người dùng để mời.
     */
    @Nested
    class SearchUsersForInviteTests {

        @Test
        @DisplayName("HT_TK_30 - Đảm bảo tìm kiếm user để mời vào lớp loại trừ current user và đúng vai trò.")
        void searchUsersForInvite_Success() {
            User currentUser = user(99L, "current", "current@example.com", "CURRENT", Role.TEACHER);
            when(authService.getCurrentUser()).thenReturn(currentUser);

            when(userRepository.findUsersForInvite(
                    eq("student"),
                    eq(Role.STUDENT),
                    eq(10L),
                    eq(99L),
                    any(Pageable.class))).thenReturn(new PageImpl<>(
                            List.of(
                                    user(1L, "student01", "student01@example.com", "SV001", Role.STUDENT))));

            ResponseListData<UserSearchForInviteResponse> response = service
                    .searchUsersForInvite(inviteSearchRequest("student", Role.STUDENT), 10L);

            assertNotNull(response);

            verify(userRepository).findUsersForInvite(
                    eq("student"),
                    eq(Role.STUDENT),
                    eq(10L),
                    eq(99L),
                    any(Pageable.class));
        }
    }
}