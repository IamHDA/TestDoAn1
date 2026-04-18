package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.session.SubmitAnswerRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.session.NextQuestionResponse;
import com.vn.backend.dto.response.session.SubmitAnswerResponse;
import com.vn.backend.services.AdaptiveSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConst.API + "/session")
@RequiredArgsConstructor
@Tag(name = "Adaptive Session", description = "APIs for adaptive learning session")
public class AdaptiveSessionController extends BaseController {

    private final AdaptiveSessionService adaptiveSessionService;

    @GetMapping("/next-question")
    @Operation(summary = "Get next question", description = "Get next adaptive question based on user mastery and prerequisites")
    public AppResponse<NextQuestionResponse> getNextQuestion(@RequestParam Long subjectId) {
        log.info("Received request to get next question for subjectId: {}", subjectId);
        NextQuestionResponse response = adaptiveSessionService.getNextQuestion(subjectId);
        log.info("Successfully retrieved next question");
        return success(response);
    }

    @PostMapping("/submit-answer")
    @Operation(summary = "Submit answer", description = "Submit answer and update mastery score")
    public AppResponse<SubmitAnswerResponse> submitAnswer(@Valid @RequestBody SubmitAnswerRequest request) {
        log.info("Received request to submit answer for questionId: {}", request.getQuestionId());
        SubmitAnswerResponse response = adaptiveSessionService.submitAnswer(request);
        log.info("Successfully processed answer submission");
        return success(response);
    }

}

