package com.vn.backend.dto.redis;

import com.vn.backend.enums.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationHistoryRecordDTO {
  private ViolationType violationType;
  private String description;
  private String timestamp;
  private Integer violationOrder;
}
