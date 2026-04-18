package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.classroom.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.classroom.*;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.services.ClassroomService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping(AppConst.API + "/classrooms")
@RestController
public class ClassroomController extends BaseController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping("/create")
    public AppResponse<Void> createClassroom(
            @RequestBody
            @Valid
            ClassroomCreateRequest request
    ) {
        log.info("Received request to create classroom");
        classroomService.createClassroom(request);
        log.info("Successfully created classroom");
        return success(null);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<ClassroomSearchResponse>> searchClassroom(
            @RequestBody
            @Valid
            BaseFilterSearchRequest<ClassroomSearchRequest> request
    ) {
        log.info("Received request to search classroom");
        ResponseListData<ClassroomSearchResponse> responseListData = classroomService.searchClassroom(request);
        log.info("Successfully search classroom");
        return successListData(responseListData);
    }

    @GetMapping("/detail/{classroomId}")
    public AppResponse<ClassroomDetailResponse> getDetailClassroom(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID) String classroomId
    ){
        log.info("Received request to get detail classroom");
        ClassroomDetailResponse response = classroomService.getDetailClassroom(classroomId);
        log.info("Successfully get detail classroom");
        return success(response);
    }

    @GetMapping("/{classroomId}/header")
    public AppResponse<ClassroomHeaderResponse> getClassroomHeader(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID) String classroomId
    ){
        log.info("Received request to get classroom header");
        ClassroomHeaderResponse response = classroomService.getClassroomHeader(classroomId);
        log.info("Successfully get classroom header");
        return success(response);
    }

    @PutMapping("/update/{classroomId}")
    public AppResponse<Void> updateClassroom(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID) String classroomId,
        @RequestBody @Valid ClassroomUpdateRequest request
    ){
        log.info("Received request to update class room");
        classroomService.updateClassroom(classroomId, request);
        log.info("Successfully updated class room");
        return success(null);
    }

    @PutMapping("/reset-class-code/{classroomId}")
    public AppResponse<Void> resetClassCode(
        @PathVariable @Valid @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID)
        String classroomId) {
        log.info("Received request to reset class code");
        classroomService.resetClassCode(Long.parseLong(classroomId));
        log.info("Successfully request to reset class code");
        return success(null);
    }

    @GetMapping("/setting/{classroomId}/detail")
    public AppResponse<ClassroomSettingDetailResponse> getClassroomSettingDetail(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID)
        String classroomId
    ){
        log.info("Received request to get detail classroom setting");
        ClassroomSettingDetailResponse response = classroomService.getDetailClassroomSetting(classroomId);
        log.info("Successfully get detail classroom setting");
        return success(response);
    }

    @PutMapping("/setting/{classroomId}/update")
    public AppResponse<Void> updateClassroomSetting(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.CLASSROOM_ID)
        String classroomId,
        @RequestBody @Valid ClassroomSettingUpdateRequest request
    ){
        log.info("Received request to update classroom setting");
        classroomService.updateClassroomSetting(classroomId, request);
        log.info("Successfully request to update classroom setting");
        return success(null);
    }

    @PostMapping("/member/search")
    public AppResponse<ResponseListData<ClassMemberSearchResponse>> searchClassMember(
        @RequestBody
        @Valid BaseFilterSearchRequest<ClassMemberSearchRequest> request
    ){
        log.info("Received request to search class member");
        ResponseListData<ClassMemberSearchResponse> response = classroomService.searchClassMember(request);
        log.info("Successfully search class member");
        return successListData(response);
    }

    @PutMapping("/member/{memberId}/update-status")
    public AppResponse<Void> updateClassMemberStatus(
        @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT ,fieldName = FieldConst.MEMBER_ID)
        String memberId,
        @RequestBody @Valid ClassMemberStatusUpdateRequest request
    ){
        log.info("Received request to update class member status");
        classroomService.updateClassMemberStatus(memberId, request);
        log.info("Successfully updated class member status");
        return success(null);
    }
}
