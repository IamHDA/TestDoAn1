package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.request.submission.SubmissionSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionUpdateRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.submission.SubmissionDetailResponse;
import com.vn.backend.dto.response.submission.SubmissionSearchResponse;
import com.vn.backend.services.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping(AppConst.API + "/submissions")
public class SubmissionController extends BaseController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/detail/{submissionId}")
    public AppResponse<SubmissionDetailResponse> getDetailSubmission(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SUBMISSION_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String submissionId
    ) {
        log.info("Received request to get detail submission");
        SubmissionDetailResponse response = submissionService.getDetailSubmission(submissionId);
        log.info("Successfully get detail submission");
        return success(response);
    }

    @GetMapping("/my-submission/{assignmentId}")
    public AppResponse<SubmissionDetailResponse> getMySubmission(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.ASSIGNMENT_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String assignmentId
    ) {
        log.info("Received request to get my submission for assignment {}", assignmentId);
        SubmissionDetailResponse response = submissionService.getMySubmission(assignmentId);
        log.info("Successfully retrieved my submission for assignment {}", assignmentId);
        return success(response);
    }


    @PutMapping("/update/{submissionId}")
    public AppResponse<Void> addAttachmentToSubmission(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SUBMISSION_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String submissionId,
            @RequestBody @Valid SubmissionUpdateRequest request
    ) {
        log.info("Received request to update submission");
        submissionService.addAttachmentToSubmission(submissionId, request);
        log.info("Successfully updated submission");
        return success(null);
    }

    @DeleteMapping("/delete/attachment/{attachmentId}")
    public AppResponse<Void> deleteAttachment(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.ATTACHMENT_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String attachmentId
    ) {
        log.info("Received request to delete submission");
        submissionService.deleteAttachmentInSubmission(attachmentId);
        log.info("Successfully deleted submission");
        return success(null);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<SubmissionSearchResponse>> searchSubmission(
            @RequestBody @Valid BaseFilterSearchRequest<SubmissionSearchRequest> request
    ) {
        log.info("Received request to search submissions");
        ResponseListData<SubmissionSearchResponse> response = submissionService.searchSubmission(request);
        log.info("Successfully searched submissions");
        return successListData(response);
    }

    @PutMapping("/mark/{submissionId}")
    public AppResponse<Void> markSubmission(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SUBMISSION_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String submissionId,
            @RequestBody @Valid SubmissionGradeUpdateRequest request
    ) {
        log.info("Received request to mark submission");
        submissionService.markSubmission(submissionId, request);
        log.info("Successfully marked submission");
        return success(null);
    }

    @GetMapping("/download-all/{assignmentId}")
    public ResponseEntity<Resource> downloadAllSubmissions(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.ASSIGNMENT_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String assignmentId
    ) {
        log.info("Received request to download all submissions");
        Resource zipFile = submissionService.downloadAllSubmissions(assignmentId);
        log.info("Successfully download all submissions");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=submissions_" + assignmentId + ".zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipFile);
    }

    @GetMapping("/{assignmentId}/download/grade/template")
    public ResponseEntity<Resource> downloadGradeTemplate(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.ASSIGNMENT_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String assignmentId
    ) {
        log.info("Received request to download grade template");
        Resource resource = submissionService.downloadGradeTemplate(assignmentId);
        log.info("Successfully download grade template");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=submission_assignment_" + assignmentId + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }

    @PostMapping("/{assignmentId}/grade/import")
    public AppResponse<Void> importSubmissionScores(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.ASSIGNMENT_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String assignmentId,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Received request to import submission scores");
        submissionService.importSubmissionScoresFromExcel(assignmentId, file);
        log.info("Successfully import submission scores");
        return success(null);
    }

}
