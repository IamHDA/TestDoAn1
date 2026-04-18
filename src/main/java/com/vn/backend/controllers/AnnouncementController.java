package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementListRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.services.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping(AppConst.API + "/announcements")
@RestController
@RequiredArgsConstructor
@Tag(name = "Announcement Management", description = "APIs for managing announcements")
public class AnnouncementController extends BaseController {
    
    private final AnnouncementService announcementService;

    @PostMapping("/create/{classroomId}")
    @Operation(summary = "Create announcement", description = "Create a new announcement in a classroom")
    public AppResponse<Void> createAnnouncement(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.CLASSROOM_ID) String classroomId,
            @Valid @RequestBody AnnouncementCreateRequest request) {
        log.info("Received request to create announcement for classroom: {}", classroomId);
        announcementService.createAnnouncement(Long.parseLong(classroomId), request);
        log.info("Successfully created announcement");
        return success(null);
    }

    @PostMapping("/list/{classroomId}")
    @Operation(summary = "Get announcement list", description = "Get paginated list of announcements for a classroom")
    public AppResponse<ResponseListData<AnnouncementResponse>> getAnnouncementList(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.CLASSROOM_ID) String classroomId,
            @Valid @RequestBody AnnouncementListRequest request) {
        log.info("Received request to get announcement list for classroom: {}", classroomId);
        ResponseListData<AnnouncementResponse> response = announcementService.getAnnouncementList(Long.parseLong(classroomId), request);
        log.info("Successfully retrieved announcement list");
        return successListData(response);
    }

    @GetMapping("/generic/detail/{announcementId}")
    @Operation(summary = "Get announcement detail", description = "Get detailed information of a specific announcement")
    public AppResponse<AnnouncementResponse> getAnnouncementDetail(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ANNOUNCEMENT_ID) String announcementId) {
        log.info("Received request to get announcement detail: {}", announcementId);
        AnnouncementResponse response = announcementService.getAnnouncementDetail(Long.parseLong(announcementId));
        log.info("Successfully retrieved announcement detail");
        return success(response);
    }

    @PutMapping("/update/{announcementId}")
    @Operation(summary = "Update announcement", description = "Update an existing announcement")
    public AppResponse<Void> updateAnnouncement(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ANNOUNCEMENT_ID) String announcementId,
            @Valid @RequestBody AnnouncementUpdateRequest request) {
        log.info("Received request to update announcement: {}", announcementId);
        announcementService.updateAnnouncement(Long.parseLong(announcementId), request);
        log.info("Successfully updated announcement: {}", announcementId);
        return success(null);
    }

    @PostMapping("/delete/{announcementId}")
    @Operation(summary = "Delete announcement", description = "Delete an announcement")
    public AppResponse<Void> deleteAnnouncement(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ANNOUNCEMENT_ID) String announcementId) {
        log.info("Received request to delete announcement: {}", announcementId);
        announcementService.deleteAnnouncement(Long.parseLong(announcementId));
        log.info("Successfully deleted announcement: {}", announcementId);
        return success(null);
    }

}
