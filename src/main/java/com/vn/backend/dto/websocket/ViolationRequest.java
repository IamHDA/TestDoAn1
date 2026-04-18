package com.vn.backend.dto.websocket;

import com.vn.backend.enums.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRequest {

  private Long studentId;
  private ViolationType violationType;
  private Integer violationCount;
  private String description;
}