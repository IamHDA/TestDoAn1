package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.topic.CreateApprovalTopicRequest;
import com.vn.backend.dto.request.topic.CreateTopicRequest;
import com.vn.backend.dto.request.topic.TopicFilterRequest;
import com.vn.backend.dto.request.topic.UpdateTopicRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.session.PracticeSetResponse;
import com.vn.backend.dto.response.topic.TopicDetailResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.services.AdaptiveSessionService;
import com.vn.backend.services.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConst.API + "/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Management", description = "APIs for managing topics by subject")
public class TopicController extends BaseController {

    private final TopicService topicService;
    private final AdaptiveSessionService adaptiveSessionService;

    @PostMapping("/create")
    @Operation(summary = "Create topic")
    public AppResponse<Void> create(@Valid @RequestBody CreateApprovalTopicRequest request) {
        topicService.approvalRequestTopic(request);
        return success(null);
    }

    @PutMapping("/{topicId}/update")
    @Operation(summary = "Update topic")
    public AppResponse<Void> update(@PathVariable Long topicId, @Valid @RequestBody UpdateTopicRequest request) {
        topicService.updateTopic(topicId, request);
        return success(null);
    }

    @DeleteMapping("/{topicId}")
    @Operation(summary = "Delete topic")
    public AppResponse<Void> delete(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return success(null);
    }


    @PostMapping("/search")
    @Operation(summary = "Search topics", description = "Search topics with filtering by subjectId")
    public AppResponse<ResponseListData<TopicResponse>> searchTopics(
            @RequestBody @Valid BaseFilterSearchRequest<TopicFilterRequest> request) {
        log.info("Received request to search topics");
        ResponseListData<TopicResponse> responseListData = topicService.searchTopics(request);
        log.info("Successfully searched topics");
        return successListData(responseListData);
    }

    @GetMapping("/{topicId}/practice-set")
    @Operation(summary = "Get practice set", description = "Get all questions for a mastered topic (Practice Mode - includes correct answers). Only available for topics with mastery >= 0.8")
    public AppResponse<PracticeSetResponse> getPracticeSet(@PathVariable Long topicId) {
        log.info("Received request to get practice set for topicId: {}", topicId);
        PracticeSetResponse response = adaptiveSessionService.getPracticeSet(topicId);
        log.info("Successfully retrieved practice set for topicId: {}", topicId);
        return success(response);
    }
}


