package com.vn.backend.dto.request.assignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssigneeAddRequestDTO {
  private Long userId;
}
