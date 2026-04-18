package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.user.CreateUserRequest;
import com.vn.backend.dto.request.user.UpdateUserRequest;
import com.vn.backend.dto.request.user.UserFilterRequest;
import com.vn.backend.dto.request.user.UserSearchForInviteFilterRequest;
import com.vn.backend.dto.request.user.UserSearchForInviteRequest;
import com.vn.backend.dto.request.user.UserSearchRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.user.UserSearchForInviteResponse;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.enums.StatusUser;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.UserService;
import com.vn.backend.utils.ExcelUtils;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.SearchUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends BaseService implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserServiceImpl(MessageUtils messageUtils, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        super(messageUtils);
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Override
    @Transactional
    public void createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsernameAndIsActive(request.getUsername(), true).isPresent()) {
            throw new AppException(AppConst.MessageConst.USERNAME_ALREADY_EXISTS,messageUtils.getMessage(AppConst.MessageConst.USERNAME_ALREADY_EXISTS),HttpStatus.BAD_REQUEST);
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(AppConst.MessageConst.EMAIL_ALREADY_EXISTS,messageUtils.getMessage(AppConst.MessageConst.EMAIL_ALREADY_EXISTS),HttpStatus.BAD_REQUEST);
        }

        if(userRepository.findByCodeAndIsActive(request.getCode(),true).isPresent()){
            throw new AppException(AppConst.MessageConst.CODE_ALREADY_EXISTS,messageUtils.getMessage(AppConst.MessageConst.CODE_ALREADY_EXISTS),HttpStatus.BAD_REQUEST);
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .code(request.getCode())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .role(request.getRole())
                .isActive(true)
                .isDeleted(false)
                .build();
        
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.USER_NOT_FOUND,messageUtils.getMessage(AppConst.MessageConst.USER_NOT_FOUND, id),HttpStatus.BAD_REQUEST));
        
        // Check if username already exists (excluding current user)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsernameAndIsActive(request.getUsername(), true).isPresent()) {
                throw new AppException(AppConst.MessageConst.USERNAME_ALREADY_EXISTS,messageUtils.getMessage(AppConst.MessageConst.USERNAME_ALREADY_EXISTS),HttpStatus.BAD_REQUEST);
            }
            user.setUsername(request.getUsername());
        }
        
        // Check if email already exists (excluding current user)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AppException(AppConst.MessageConst.EMAIL_ALREADY_EXISTS,messageUtils.getMessage(AppConst.MessageConst.EMAIL_ALREADY_EXISTS),HttpStatus.BAD_REQUEST);
            }
            user.setEmail(request.getEmail());
        }
        if(request.getPassword()!=null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Update fields if provided
        if (request.getCode() != null) user.setCode(request.getCode());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());

        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void updateUserStatus(Long id, StatusUser status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.USER_NOT_FOUND,messageUtils.getMessage(AppConst.MessageConst.USER_NOT_FOUND, id),HttpStatus.BAD_REQUEST));
        
        user.setIsActive(status.getValue());

        userRepository.save(user);
    }
    
    @Override
    public ResponseListData<UserResponse> getUsers(UserSearchRequest request) {
        // Extract filters from request
        UserFilterRequest filters = request.getFilters() != null ? request.getFilters() : new UserFilterRequest();
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<User> userPage;
        // Check if any filters are provided
        boolean hasFilters = (filters.getSearch() != null && !filters.getSearch().trim().isEmpty()) ||
                           filters.getRole() != null ||
                           filters.getStatus() != null;
        
        if (hasFilters) {
            String searchTerm = (filters.getSearch() != null && !filters.getSearch().trim().isEmpty())
            ? SearchUtils.getLikeValue(filters.getSearch().trim()) : null;
            Boolean status = filters.getStatus() != null ? filters.getStatus().getValue() : null;
            
            userPage = userRepository.findByFilters(searchTerm, filters.getRole(), status, pageable);
        } else {
            userPage = userRepository.findUsers(pageable);
        }
        
        List<UserResponse> users = userPage.getContent()
                .stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(userPage.getTotalElements());
        pagingMeta.setTotalPages(userPage.getTotalPages());

        return new ResponseListData<>(users, pagingMeta);
    }
    
    // ================= Excel Import/Export =================
    private static final String[] USER_IMPORT_HEADERS = new String[] {
            "STT", "Full Name", "Code", "Email", "Phone", "Date of Birth (dd/MM/yyyy)", "Gender", "Address", "Role"
    };
    private static final String[] USER_IMPORT_FIELDS = new String[] {
            "index", "fullName", "code", "email", "phone", "dateOfBirth", "gender", "address", "role"
    };

    @Override
    public ByteArrayResource downloadUserImportTemplate() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("index", 1);
        example.put("fullName", "");
        example.put("code", "");
        example.put("email", "");
        example.put("phone", "");
        example.put("dateOfBirth", "dd/MM/yyyy");
        example.put("gender", "");
        example.put("address", "");
        example.put("role", "STUDENT|TEACHER|ADMIN");
        rows.add(example);

        List<UserTemplateRow> data = rows.stream().map(UserTemplateRow::fromMap).toList();
        return ExcelUtils.exportToExcel(
                data,
                "USER_IMPORT",
                USER_IMPORT_HEADERS,
                USER_IMPORT_FIELDS
        );
    }

    @Override
    @Transactional
    public void importUsersFromExcel(MultipartFile file) {
        List<UserImportRow> importRows = parseUserExcel(file);
        // Validate duplicates and existing records before persisting
        validateUserImportRows(importRows);

        for (UserImportRow r : importRows) {
            if (r.email == null || r.email.isBlank()) {
                continue;
            }
            if (userRepository.findByEmail(r.email).isPresent()) continue;

            String plainPassword = generatePassword(r.fullName, r.dateOfBirth);
            String encodedPassword = passwordEncoder.encode(plainPassword);

            String codeVal = (r.code != null && !r.code.isBlank()) ? r.code : generateUniqueCode(r.fullName);
            String usernameVal = codeVal;
            int unameSuffix = 1;
            while (userRepository.findByUsernameAndIsActive(usernameVal, true).isPresent()) {
                usernameVal = codeVal + unameSuffix++;
            }

            Role roleParsed = parseRole(r.roleRaw);

            User user = User.builder()
                    .username(usernameVal)
                    .code(codeVal)
                    .email(r.email)
                    .password(encodedPassword)
                    .fullName(r.fullName)
                    .phone(r.phone)
                    .dateOfBirth(r.dateOfBirth)
                    .gender(r.gender)
                    .address(r.address)
                    .role(roleParsed != null ? roleParsed : Role.STUDENT)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            userRepository.save(user);
        }
    }

    private List<UserImportRow> parseUserExcel(MultipartFile file) {
        List<UserImportRow> results = new ArrayList<>();
        try (Workbook workbook = ExcelUtils.createWorkbook(file)) {
            Sheet sheet = ExcelUtils.getSheet(workbook, 0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = ExcelUtils.getRow(sheet, i);
                if (row == null) continue;

                String fullName = ExcelUtils.getCellValueAsString(row.getCell(1));
                String code = ExcelUtils.getCellValueAsString(row.getCell(2));
                String email = ExcelUtils.getCellValueAsString(row.getCell(3));
                String phone = ExcelUtils.getCellValueAsString(row.getCell(4));
                String dobStr = ExcelUtils.getCellValueAsString(row.getCell(5));
                String gender = ExcelUtils.getCellValueAsString(row.getCell(6));
                String address = ExcelUtils.getCellValueAsString(row.getCell(7));
                String roleStr = ExcelUtils.getCellValueAsString(row.getCell(8));

                LocalDate dob = parseDob(dobStr);
                results.add(new UserImportRow(i, fullName, code, email, phone, dob, gender, address, roleStr));
            }
        } catch (java.io.IOException e) {
            throw new AppException(
                    AppConst.MessageConst.FILE_UPLOAD_FAILED,
                    messageUtils.getMessage(AppConst.MessageConst.FILE_UPLOAD_FAILED),
                    HttpStatus.BAD_REQUEST
            );
        }
        return results;
    }

    private void validateUserImportRows(List<UserImportRow> rows) {
        List<String> errors = new ArrayList<>();
        // Track duplicates inside the file
        Set<String> seenCodes = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (UserImportRow r : rows) {
            int stt = r.stt;
            // Required fields
            if (r.fullName == null || r.fullName.isBlank()) {
                errors.add("STT " + stt + ": FULL_NAME không được để trống");
            }
            if (r.email == null || r.email.isBlank()) {
                errors.add("STT " + stt + ": EMAIL không được để trống");
            }
            if (r.roleRaw == null || r.roleRaw.isBlank()) {
                errors.add("STT " + stt + ": ROLE không được để trống");
            } else if (!isValidRole(r.roleRaw)) {
                errors.add("STT " + stt + ": Role không hợp lệ (" + r.roleRaw + ")");
            }
            if (r.code != null && !r.code.isBlank()) {
                String normCode = r.code.trim();
                if (!seenCodes.add(normCode)) {
                    errors.add("STT " + stt + ": CODE trùng trong file: " + normCode);
                }
                if (userRepository.findByCodeAndIsActive(normCode, true).isPresent()) {
                    errors.add("STT " + stt + ": CODE đã tồn tại trong hệ thống: " + normCode);
                }
            }
            if (r.email != null && !r.email.isBlank()) {
                String normEmail = r.email.trim().toLowerCase();
                if (!seenEmails.add(normEmail)) {
                    errors.add("STT " + stt + ": EMAIL trùng trong file: " + normEmail);
                }
                if (userRepository.findByEmail(normEmail).isPresent()) {
                    errors.add("STT " + stt + ": EMAIL đã tồn tại trong hệ thống: " + normEmail);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new AppException(
                    AppConst.MessageConst.FILE_UPLOAD_FAILED,
                    String.join("\n", errors),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private LocalDate parseDob(String s) {
        if (s == null || s.isBlank()) return null;
        List<String> fmts = List.of("dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd");
        for (String f : fmts) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern(f)); } catch (DateTimeParseException ignored) {}
        }
        try { return LocalDate.parse(s.substring(0, 10)); } catch (Exception ignored) {}
        return null;
    }

    private boolean isValidRole(String s) {
        if (s == null || s.isBlank()) return false;
        String v = s.trim().toUpperCase();
        return Role.allowedNames().contains(v);
    }

    private Role parseRole(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toUpperCase();
        if (!isValidRole(v)) return null;
        return Role.valueOf(v);
    }


    private String generatePassword(String fullName, LocalDate dob) {
        if (dob != null) {
            return dob.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        }
        String base = toBaseUserFromFullName(fullName);
        if (base.isEmpty()) base = randomAlphaNum(6);
        return base + "1";
    }

    private String generateUniqueCode(String fullName) {
        String base = toBaseUserFromFullName(fullName);
        if (base.isEmpty()) base = randomAlphaNum(6);
        // get existing codes starting with base
        List<String> existing = userRepository.findCodesByPrefix(base);
        int max = 0;
        for (String code : existing) {
            if (code == null) continue;
            String lower = code.toLowerCase();
            if (!lower.startsWith(base)) continue;
            String tail = lower.substring(base.length());
            if (tail.matches("\\d+")) {
                try {
                    int val = Integer.parseInt(tail);
                    if (val > max) max = val;
                } catch (NumberFormatException ignored) {}
            }
        }
        return base + (max + 1);
    }

    private String toBaseUserFromFullName(String fullName) {
        if (fullName == null) return "";
        String normalized = removeDiacritics(fullName).trim().toLowerCase();
        if (normalized.isEmpty()) return "";
        String[] parts = normalized.split("\\s+");
        if (parts.length == 0) return "";
        String last = parts[parts.length - 1];
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isBlank()) initials.append(parts[i].charAt(0));
        }
        return last + initials.toString();
    }

    private String removeDiacritics(String input) {
        if (input == null) return "";
        String nfd = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String without = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        without = without.replace("đ", "d").replace("Đ", "D");
        return without.replaceAll("[^a-zA-Z\\s]", " ");
    }

    private String randomAlphaNum(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // Beans for Excel export/import mapping
    private static class UserTemplateRow {
        public Integer index;
        public String fullName;
        public String code;
        public String email;
        public String phone;
        public String dateOfBirth;
        public String gender;
        public String address;
        public String role;

        static UserTemplateRow fromMap(Map<String, Object> m) {
            UserTemplateRow r = new UserTemplateRow();
            r.index = (Integer) m.get("index");
            r.fullName = (String) m.get("fullName");
            r.code = (String) m.get("code");
            r.email = (String) m.get("email");
            r.phone = (String) m.get("phone");
            r.dateOfBirth = (String) m.get("dateOfBirth");
            r.gender = (String) m.get("gender");
            r.address = (String) m.get("address");
            r.role = (String) m.get("role");
            return r;
        }
    }

    private record UserImportRow(
            int stt,
            String fullName,
            String code,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String roleRaw
    ) {}


    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.USER_NOT_FOUND,messageUtils.getMessage(AppConst.MessageConst.USER_NOT_FOUND,id),HttpStatus.BAD_REQUEST));

        return UserResponse.fromUser(user);
    }

    @Override
    public ResponseListData<UserSearchForInviteResponse> searchUsersForInvite(UserSearchForInviteRequest request, Long classroomId) {
        UserSearchForInviteFilterRequest filters = request.getFilters();
        Long currentUserId = authService.getCurrentUser().getId();
        // Build pageable from request
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        
        Page<User> userPage = userRepository.findUsersForInvite(
            filters.getSearch(),
            filters.getRole(),
            classroomId,
                currentUserId,
            pageable
        );
        
        List<UserSearchForInviteResponse> users = userPage.getContent()
                .stream()
                .map(this::convertToUserSearchForInviteResponse)
                .collect(Collectors.toList());

        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(userPage.getTotalElements());
        pagingMeta.setTotalPages(userPage.getTotalPages());

        return new ResponseListData<>(users, pagingMeta);
    }

    private UserSearchForInviteResponse convertToUserSearchForInviteResponse(User user) {
        return UserSearchForInviteResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

}
