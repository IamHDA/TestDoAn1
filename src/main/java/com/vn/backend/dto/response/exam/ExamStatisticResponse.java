package com.vn.backend.dto.response.exam;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamStatisticResponse {

  private Integer totalQuestion;
  private List<DifficultyDistributionResponse> difficultyDistribution;
  private List<TopicDistributionResponse> topicDistribution;
}
