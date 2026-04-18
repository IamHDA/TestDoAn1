package com.vn.backend.dto.request.assignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssigneeSearchRequestDTO {

  private String username;
  private String fullName;
  private Long assignmentId;
}
