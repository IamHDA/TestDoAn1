package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.comment.CommentCreateRequest;
import com.vn.backend.dto.request.comment.CommentListRequest;
import com.vn.backend.dto.request.comment.CommentUpdateRequest;
import com.vn.backend.dto.request.comment.CommentDeleteRequest;
import com.vn.backend.dto.response.comment.CommentResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.services.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping(AppConst.API + "/comments")
@RestController
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "APIs for managing comments on announcements")
public class CommentController extends BaseController {

    private final CommentService commentService;

    @PostMapping("/create/{announcementId}")
    @Operation(summary = "Create comment", description = "Create a new comment for an announcement")
    public AppResponse<CommentResponse> createComment(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ANNOUNCEMENT_ID) String announcementId,
            @Valid @RequestBody CommentCreateRequest request) {
        log.info("Received request to create comment for announcement: {}", announcementId);
        CommentResponse commentResponse = commentService.createComment(Long.parseLong(announcementId), request);
        log.info("Successfully created comment");
        return success(commentResponse);
    }

    @PostMapping("/list/{announcementId}")
    @Operation(summary = "Get comment list", description = "Get paginated list of comments for an announcement")
    public AppResponse<ResponseListData<CommentResponse>> getCommentList(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.ANNOUNCEMENT_ID) String announcementId,
            @Valid @RequestBody CommentListRequest request) {
        log.info("Received request to get comment list for announcement: {}", announcementId);
        request.setAnnouncementId(Long.parseLong(announcementId));
        ResponseListData<CommentResponse> response = commentService.getCommentList(request);
        log.info("Successfully retrieved comment list");
        return successListData(response);
    }

    @PutMapping("/update")
    @Operation(summary = "Update comment", description = "Update an existing comment")
    public AppResponse<CommentResponse> updateComment(@Valid @RequestBody CommentUpdateRequest request) {
        log.info("Received request to update comment: {}", request.getCommentId());
        CommentResponse commentResponse = commentService.updateComment(request);
        log.info("Successfully updated comment: {}", request.getCommentId());
        return success(commentResponse);
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete comment", description = "Delete a comment (soft delete)")
    public AppResponse<Void> deleteComment(@Valid @RequestBody CommentDeleteRequest request) {
        log.info("Received request to delete comment: {}", request.getCommentId());
        commentService.deleteComment(request);
        log.info("Successfully deleted comment: {}", request.getCommentId());
        return success(null);
    }
}
