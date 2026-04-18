package com.vn.backend.dto.response.sessionexam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveAnswersResponse {
  private Long savedAt;
  private Integer totalAnswered;
}
