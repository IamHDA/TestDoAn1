package com.vn.backend.dto.response.sessionexam;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingResult {

  private Double score;

  private Integer correctCount;

  private Integer totalQuestions;

  private Map<Long, Boolean> correctnessMap;
}
