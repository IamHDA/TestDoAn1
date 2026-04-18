package com.vn.backend.dto.response.assignment;

import com.vn.backend.entities.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssigneeSearchQueryDTO {

  private User user;
  private boolean isAssigned;
}

