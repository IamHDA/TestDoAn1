package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.answer.AnswerUpdateRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.services.AnswerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequestMapping(AppConst.API + "/answers")
public class AnswerController extends BaseController {

    @Autowired
    private AnswerService answerService;

    @PostMapping("/create")
    public AppResponse<AnswerResponse> addAnswer(@RequestBody @Valid AnswerCreateRequest request) {
        log.info("Received request to create answer");
        AnswerResponse answer = answerService.addAnswer(request);
        log.info("Successfully created answer");
        return success(answer);
    }

    @PutMapping("/update/{answerId}")
    public AppResponse<AnswerResponse> updateAnswer(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SORT_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String answerId,
            @RequestBody @Valid AnswerUpdateRequest request
    ) {
        log.info("Received request to update answer");
        AnswerResponse answer = answerService.updateAnswer(Long.valueOf(answerId), request);
        log.info("Successfully updated answer");
        return success(answer);
    }

    @DeleteMapping("/delete/{answerId}")
    public AppResponse<Void> softDeleteAnswer(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SORT_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
            String answerId) {
        log.info("Received request to delete answer");
        answerService.softDeleteAnswer(Long.valueOf(answerId));
        log.info("Successfully deleted answer");
        return success(null);
    }

    @PostMapping("/reorder")
    public AppResponse<Void> reorderAnswers(@RequestParam Long questionId, @RequestBody List<Long> answerOrderIds) {
        log.info("Received request to reorder answers for questionId={}", questionId);
        answerService.reorderAnswers(questionId, answerOrderIds);
        log.info("Successfully reordered answers for questionId={}", questionId);
        return success(null);
    }
}
