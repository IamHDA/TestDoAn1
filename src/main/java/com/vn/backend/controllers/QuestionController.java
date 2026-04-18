package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.question.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.question.QuestionSearchResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.services.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Validated
@RestController
@RequestMapping(AppConst.API + "/questions")
public class QuestionController extends BaseController {
    @Autowired
    private QuestionService questionService;

    @PostMapping("/create")
    public AppResponse<QuestionDetailResponse> createQuestion(@RequestBody @Valid QuestionCreateRequest request) {
        log.info("Received request to create question");
        questionService.createQuestion(request);
        log.info("Successfully created question");
        return success(null);
    }

    @PostMapping("/bulk-create")
    public AppResponse<List<QuestionDetailResponse>> bulkCreateQuestions(
            @RequestBody @Valid QuestionBulkCreateRequest request
    ) {
        log.info("Received request to create questions in bulk");
        List<QuestionDetailResponse> responses = questionService.createQuestions(request.getQuestions());
        log.info("Successfully created questions in bulk");
        return success(responses);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<QuestionSearchResponse>> searchQuestions(@RequestBody @Valid BaseFilterSearchRequest<QuestionSearchRequest> request) {
        log.info("Received request to search questions");
        ResponseListData<QuestionSearchResponse> resp = questionService.searchQuestions(request);
        log.info("Searched questions successfully");
        return successListData(resp);
    }

    @GetMapping("/detail/{questionId}")
    public AppResponse<QuestionDetailResponse> getQuestionDetail(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SORT_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String questionId) {
        log.info("Received request to get question detail");
        QuestionDetailResponse resp = questionService.getQuestionDetail(Long.valueOf(questionId));
        log.info("Successfully get question detail");
        return success(resp);
    }

    @PutMapping("/update/{questionId}")
    public AppResponse<QuestionDetailResponse> updateQuestion(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SORT_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String questionId,
            @RequestBody @Valid QuestionUpdateRequest request
    ) {
        log.info("Received request to update question");
        QuestionDetailResponse result = questionService.updateQuestion(Long.valueOf(questionId), request);
        log.info("Successfully updated question");
        return success(result);
    }

    @DeleteMapping("/delete/{questionId}")
    public AppResponse<Void> softDeleteQuestion(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SORT_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String questionId
    ) {
        log.info("Received request to delete question");
        questionService.softDeleteQuestion(Long.valueOf(questionId));
        log.info("Successfully deleted question");
        return success(null);
    }

    @PostMapping("/import-excel")
    public AppResponse<QuestionBulkCreateRequest> importQuestionsFromExcel(@RequestParam("file") MultipartFile file,
                                         @ModelAttribute QuestionImportExcelRequest request) {
        log.info("Received request to import questions from excel");
        QuestionBulkCreateRequest result = questionService.importQuestionsFromExcel(file, request);
        log.info("Successfully imported questions from excel");
        return success(result);
    }

    @PostMapping("/export-excel")
    public ResponseEntity<byte[]> exportQuestionsToExcel(@RequestBody @Valid BaseFilterSearchRequest<QuestionSearchRequest> request) {
        log.info("Received request to export questions to excel");
        byte[] excelBytes = questionService.exportQuestionsToExcel(request.getFilters());
        log.info("Successfully exported questions to excel");
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=questions.xlsx")
                .body(excelBytes);
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        log.info("Received request to download import template");
        byte[] bytes = questionService.downloadImportTemplate();
        log.info("Successfully generated import template");
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=question_import_template.xlsx")
                .body(bytes);
    }

    @PostMapping("/approval-question")
    public AppResponse<Void> createApprovalQuestion(@RequestBody @Valid CreateApprovalQuestionRequest request) {
        log.info("Received request to create approval question");
        questionService.createApprovalQuestion(request);
        log.info("Successfully created approval question");
        return success(null);
    }
}
