package com.vn.backend.dto.response.exam;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyDistributionResponse {

  private Integer level;
  private Long count;
}
