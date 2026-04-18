package com.vn.backend.services;

import com.vn.backend.dto.request.user.CreateUserRequest;
import com.vn.backend.dto.request.user.UpdateUserRequest;
import com.vn.backend.dto.request.user.UserSearchForInviteRequest;
import com.vn.backend.dto.request.user.UserSearchRequest;
import com.vn.backend.dto.response.UserResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.user.UserSearchForInviteResponse;
import com.vn.backend.enums.StatusUser;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    void createUser(CreateUserRequest request);
    void updateUser(Long id, UpdateUserRequest request);
    void updateUserStatus(Long id, StatusUser status);
    ResponseListData<UserResponse> getUsers(UserSearchRequest request);
    UserResponse getUserById(Long id);
    
    // Tìm user để mời vào lớp
    ResponseListData<UserSearchForInviteResponse> searchUsersForInvite(UserSearchForInviteRequest request, Long classroomId);

    // Excel import/export users
    Resource downloadUserImportTemplate();
    void importUsersFromExcel(MultipartFile file);
}
