package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.learningpath.LearningPathResponse;
import com.vn.backend.dto.response.progress.MasteryProgressResponse;
import com.vn.backend.services.LearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConst.API + "/subjects")
@RequiredArgsConstructor
@Tag(name = "Learning Path", description = "APIs for learning path and mastery progress")
public class LearningPathController extends BaseController {

    private final LearningPathService learningPathService;

    @GetMapping("/{subjectId}/learning-path")
    @Operation(summary = "Get learning path", description = "Get learning path with topics, prerequisites, and user mastery status")
    public AppResponse<LearningPathResponse> getLearningPath(@PathVariable Long subjectId) {
        log.info("Received request to get learning path for subjectId: {}", subjectId);
        LearningPathResponse response = learningPathService.getLearningPath(subjectId);
        log.info("Successfully retrieved learning path for subjectId: {}", subjectId);
        return success(response);
    }
}

