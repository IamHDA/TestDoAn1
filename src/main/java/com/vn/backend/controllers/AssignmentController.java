package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.assignment.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.assignment.AssigneeSearchResponse;
import com.vn.backend.dto.response.assignment.AssignmentAverageScoreComparisonResponse;
import com.vn.backend.dto.response.assignment.AssignmentImprovementTrendResponse;
import com.vn.backend.dto.response.assignment.AssignmentListResponse;
import com.vn.backend.dto.response.assignment.AssignmentResponse;
import com.vn.backend.dto.response.assignment.AssignmentStatisticResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;
import com.vn.backend.services.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping(AppConst.API + "/assignments")
@RestController
@RequiredArgsConstructor
@Tag(name = "Assignment Management", description = "APIs for managing assignments")
public class AssignmentController extends BaseController {

    private final AssignmentService assignmentService;

    @PostMapping("/create/{classroomId}")
    @Operation(summary = "Create assignment", description = "Create a new assignment in a classroom")
    public AppResponse<Void> createAssignment(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.CLASSROOM_ID) String classroomId,
            @Valid @RequestBody AssignmentCreateRequest request) {
        log.info("Received request to create assignment for classroom: {}", classroomId);
        assignmentService.createAssignment(Long.parseLong(classroomId), request);
        log.info("Successfully created assignment");
        return success(null);
    }

    @GetMapping("/detail/{assignmentId}")
    @Operation(summary = "Get assignment detail", description = "Get detailed information of a specific assignment")
    public AppResponse<AssignmentResponse> getAssignmentDetail(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = "assignmentId") String assignmentId) {
        log.info("Received request to get assignment detail for ID: {}", assignmentId);
        AssignmentResponse response = assignmentService.getAssignmentDetail(Long.parseLong(assignmentId));
        log.info("Successfully retrieved assignment detail for ID: {}", assignmentId);
        return success(response);
    }

    @PutMapping("/update/{assignmentId}")
    @Operation(summary = "Update assignment", description = "Update an existing assignment")
    public AppResponse<Void> updateAssignment(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = "assignmentId") String assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request) {
        log.info("Received request to update assignment with ID: {}", assignmentId);
        assignmentService.updateAssignment(Long.parseLong(assignmentId), request);
        log.info("Successfully updated assignment with ID: {}", assignmentId);
        return success(null);
    }

    @PostMapping("/delete/{assignmentId}")
    @Operation(summary = "Soft delete assignment", description = "Soft delete an assignment")
    public AppResponse<Void> softDeleteAssignment(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = "assignmentId") String assignmentId) {
        log.info("Received request to soft delete assignment with ID: {}", assignmentId);
        assignmentService.softDeleteAssignment(Long.parseLong(assignmentId));
        log.info("Successfully soft deleted assignment with ID: {}", assignmentId);
        return success(null);
    }

    @PostMapping("/list/{classroomId}")
    @Operation(summary = "Get assignment list", description = "Get paginated list of assignments in a classroom")
    public AppResponse<ResponseListData<AssignmentListResponse>> getAssignmentList(
            @PathVariable @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.CLASSROOM_ID) String classroomId,
            @Valid @RequestBody AssignmentListRequest request) {
        log.info("Received request to get assignment list for classroom: {}", classroomId);
        ResponseListData<AssignmentListResponse> response = assignmentService.getAssignmentList(Long.parseLong(classroomId), request);
        log.info("Successfully retrieved assignment list for classroom: {}", classroomId);
        return successListData(response);
    }

    @PutMapping("/add-assignee/{assignmentId}")
    public AppResponse<Void> addAssignee(
            @PathVariable @AllowFormat(regex = AppConst.RegexConst.INTEGER, message = AppConst.MessageConst.INVALID_NUMBER_FORMAT, fieldName = "assignmentId") String assignmentId,
            @RequestBody @Valid AssigneeAddRequest request
    ) {
        log.info("Received request to add assignee");
        assignmentService.addAssignee(assignmentId, request);
        log.info("Successfully add assignee");
        return success(null);
    }

    @PostMapping("/search/assignee")
    public AppResponse<ResponseListData<AssigneeSearchResponse>> searchAssignee(
            @RequestBody @Valid BaseFilterSearchRequest<AssigneeSearchRequest> request
    ) {
        log.info("Received request to search assignee");
        ResponseListData<AssigneeSearchResponse> response = assignmentService.searchAssignee(request);
        log.info("Successfully search assignee");
        return successListData(response);
    }

    @GetMapping("/{assignmentId}/statistics")
    public AppResponse<AssignmentStatisticResponse> getAssignmentStatistics(
            @PathVariable
            @AllowFormat(regex = AppConst.RegexConst.INTEGER, message = AppConst.MessageConst.INVALID_NUMBER_FORMAT, fieldName = "assignmentId")
            String assignmentId) {
        log.info("Received request to get assignment statistics");
        AssignmentStatisticResponse response = assignmentService.getAssignmentStatistics(assignmentId);
        log.info("Successfully get assignment statistics");
        return success(response);
    }

    @GetMapping("/{classroomId}/average-score-comparison")
    public AppResponse<AssignmentAverageScoreComparisonResponse> getAverageScoreComparison(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.CLASSROOM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String classroomId) {
        log.info("Received request to get average score comparison for classroom {}", classroomId);
        AssignmentAverageScoreComparisonResponse response = assignmentService.getAverageScoreComparison(
                Long.valueOf(classroomId));
        log.info("Successfully got average score comparison for classroom {}", classroomId);
        return success(response);
    }

    @GetMapping("/{classroomId}/improvement-trend")
    public AppResponse<AssignmentImprovementTrendResponse> getImprovementTrend(
            @PathVariable
            @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.CLASSROOM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
            String classroomId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String groupBy) {
        log.info("Received request to get improvement trend for classroom {} with period={}, groupBy={}",
                classroomId, period, groupBy);

        // Convert String to Enum
        StatisticsPeriod periodEnum = StatisticsPeriod.from(period);
        StatisticsGroupBy groupByEnum = StatisticsGroupBy.from(groupBy);

        AssignmentImprovementTrendResponse response = assignmentService.getImprovementTrend(
                Long.valueOf(classroomId), periodEnum, groupByEnum);
        log.info("Successfully got improvement trend for classroom {}", classroomId);
        return success(response);
    }
}
