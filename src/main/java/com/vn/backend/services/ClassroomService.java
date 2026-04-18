package com.vn.backend.services;

import com.vn.backend.dto.request.classroom.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.classroom.*;
import com.vn.backend.dto.response.common.ResponseListData;

public interface ClassroomService {

    void createClassroom(ClassroomCreateRequest request);

    ResponseListData<ClassroomSearchResponse> searchClassroom(BaseFilterSearchRequest<ClassroomSearchRequest> request);

    ClassroomDetailResponse getDetailClassroom(String classroomId);

    ClassroomHeaderResponse getClassroomHeader(String classroomId);

    void updateClassroom(String classroomId, ClassroomUpdateRequest request);

    void resetClassCode(Long classroomId);

    ClassroomSettingDetailResponse getDetailClassroomSetting(String classroomId);

    void updateClassroomSetting(String classroomId, ClassroomSettingUpdateRequest request);

    ResponseListData<ClassMemberSearchResponse> searchClassMember(BaseFilterSearchRequest<ClassMemberSearchRequest> request);

    void updateClassMemberStatus(String memberId, ClassMemberStatusUpdateRequest request);
}
