package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.user.CreateUserRequest;
import com.vn.backend.dto.request.user.UpdateUserRequest;
import com.vn.backend.dto.request.user.UserSearchForInviteRequest;
import com.vn.backend.dto.request.user.UserSearchRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.user.UserSearchForInviteResponse;
import com.vn.backend.enums.StatusUser;
import com.vn.backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;


@RequestMapping(AppConst.API + "/users")
@RestController
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController extends BaseController {
    
    private final UserService userService;
    
    @PostMapping("/create")
    @Operation(summary = "Create new user", description = "Create a new user account")
    public AppResponse<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Received request to create user");
        userService.createUser(request);
        log.info("Successfully created user");
        return success(null);
    }
    
    @PutMapping("/{id}/update")
    @Operation(summary = "Update user", description = "Update user information by ID")
    public AppResponse<Void> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Received request to update user with id: {}", id);
        userService.updateUser(id, request);
        log.info("Successfully updated user with id: {}", id);
        return success(null);
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Update user status (ACTIVE/INACTIVE)")
    public AppResponse<Void> updateUserStatus(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "ACTIVE/INACTIVE") @RequestParam StatusUser status) {
        log.info("Received request to update user status with id: {} to {}", id, status);
        userService.updateUserStatus(id, status);
        log.info("Successfully updated user status with id: {} to {}", id, status);
        return success(null);
    }


    
    @PostMapping
    @Operation(summary = "Get list User", description = "Get paginated list of users with advanced filtering and sorting using BaseFilterSearchRequest")
    public AppResponse<ResponseListData<UserResponse>> getUsers(
            @Valid @RequestBody UserSearchRequest request) {
        log.info("Received request to get users list with filters");
        
        ResponseListData<UserResponse> responseListData = userService.getUsers(request);
        log.info("Successfully retrieved users list with filters");
        
        return success(responseListData);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user details by ID")
    public AppResponse<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Received request to get user with id: {}", id);
        UserResponse userResponse = userService.getUserById(id);
        log.info("Successfully retrieved user with id: {}", id);
        return success(userResponse);
    }
    
    @PostMapping("/search-for-invite/{classroomId}")
    @Operation(summary = "Search users for invitation", description = "Search users that can be invited to a classroom")
    public AppResponse<ResponseListData<UserSearchForInviteResponse>> searchUsersForInvite(
            @Parameter(description = "Classroom ID") @PathVariable @AllowFormat(
                    regex = RegexConst.INTEGER, 
                    message = MessageConst.INVALID_NUMBER_FORMAT, 
                    fieldName = FieldConst.CLASSROOM_ID) String classroomId,
            @Valid @RequestBody UserSearchForInviteRequest request) {
        log.info("Received request to search users for invitation in classroom: {}", classroomId);
        ResponseListData<UserSearchForInviteResponse> responseListData = userService.searchUsersForInvite(request, Long.parseLong(classroomId));
        log.info("Successfully searched users for invitation");
        return successListData(responseListData);
    }

    @GetMapping("/download/import-template")
    @Operation(summary = "Download user import template", description = "Tải file mẫu Excel để import người dùng")
    public ResponseEntity<Resource> downloadUserImportTemplate() {
        log.info("Received request to download user import template");
        Resource resource = userService.downloadUserImportTemplate();
        log.info("Successfully download user import template");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/import")
    @Operation(summary = "Import users from Excel", description = "Import người dùng từ file Excel. Mật khẩu: ngày sinh ddMMyyyy, nếu không có thì sinh theo họ tên binhdq{n}")
    public AppResponse<Void> importUsers(@RequestParam("file") MultipartFile file) {
        log.info("Received request to import users from excel");
        userService.importUsersFromExcel(file);
        log.info("Successfully import users from excel");
        return success(null);
    }
}