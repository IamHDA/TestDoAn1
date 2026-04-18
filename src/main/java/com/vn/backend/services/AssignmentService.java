package com.vn.backend.services;

import com.vn.backend.dto.request.assignment.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.assignment.AssigneeSearchResponse;
import com.vn.backend.dto.response.assignment.AssignmentAverageScoreComparisonResponse;
import com.vn.backend.dto.response.assignment.AssignmentImprovementTrendResponse;
import com.vn.backend.dto.response.assignment.AssignmentListResponse;
import com.vn.backend.dto.response.assignment.AssignmentResponse;
import com.vn.backend.dto.response.assignment.AssignmentStatisticResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.enums.StatisticsGroupBy;
import com.vn.backend.enums.StatisticsPeriod;

public interface AssignmentService {

    /**
     * Create a new assignment
     */
    Long createAssignment(Long classroomId, AssignmentCreateRequest request);

    /**
     * Get assignment detail by ID
     */
    AssignmentResponse getAssignmentDetail(Long assignmentId);

    /**
     * Update assignment
     */
    void updateAssignment(Long assignmentId, AssignmentUpdateRequest request);

    /**
     * Soft delete assignment
     */
    void softDeleteAssignment(Long assignmentId);

    /**
     * Get assignment list by classroom ID with pagination
     */
    ResponseListData<AssignmentListResponse> getAssignmentList(Long classroomId, AssignmentListRequest request);

    void addAssignee(String assignmentId, AssigneeAddRequest request);

    ResponseListData<AssigneeSearchResponse> searchAssignee(BaseFilterSearchRequest<AssigneeSearchRequest> request);

    AssignmentStatisticResponse getAssignmentStatistics(String assignmentId);

    AssignmentAverageScoreComparisonResponse getAverageScoreComparison(Long classroomId);

    AssignmentImprovementTrendResponse getImprovementTrend(
        Long classroomId, StatisticsPeriod period, StatisticsGroupBy groupBy);
}
