package com.vn.backend.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDistributionResponse {

  private Long topicId;
  private String topicName;
  private Long count;
}
