package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.notification.NotificationSearchResponse;
import com.vn.backend.services.NotificationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(AppConst.API + "/notifications")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/count-unread")
    public AppResponse<Long> countUnreadNotification() {
        log.info("Received request to count unread notification");
        long response = notificationService.countUnreadNotification();
        log.info("Successfully count unread notification");
        return success(response);
    }

    @PutMapping("/read/{notificationId}")
    public AppResponse<Void> updateIsReadNotification(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.VALUE_OUT_OF_RANGE, fieldName = FieldConst.NOTIFICATION_ID)
            String notificationId
    ) {
        log.info("Received request to update is read notification");
        notificationService.updateIsReadNotification(notificationId);
        log.info("Successfully update is read notification");
        return success(null);
    }

    @PutMapping("/read-all")
    public AppResponse<Void> readAllNotification() {
        log.info("Received request to update is read notification");
        notificationService.readAllNotification();
        log.info("Successfully update is read notification");
        return success(null);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<NotificationSearchResponse>> searchNotification(
            @RequestBody
            @Valid
            BaseFilterSearchRequest<Void> request
    ) {
        log.info("Received request to search notification");
        ResponseListData<NotificationSearchResponse> response = notificationService.searchNotification(request);
        log.info("Successfully search notification");
        return successListData(response);
    }
}
