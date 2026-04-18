package com.vn.backend.dto.response.assignment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssignmentStatisticResponse {
    AssignmentOverviewResponse overview;
    List<ScoreDistributionResponse> scoreDistribution;
}
