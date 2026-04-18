package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.progress.MasteryProgressResponse;
import com.vn.backend.services.LearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConst.API + "/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "APIs for user mastery progress")
public class ProgressController extends BaseController {

    private final LearningPathService learningPathService;

    @GetMapping("/mastery")
    @Operation(summary = "Get mastery progress", description = "Get all topic mastery progress for current user")
    public AppResponse<MasteryProgressResponse> getMasteryProgress() {
        log.info("Received request to get mastery progress");
        MasteryProgressResponse response = learningPathService.getMasteryProgress();
        log.info("Successfully retrieved mastery progress");
        return success(response);
    }
}

