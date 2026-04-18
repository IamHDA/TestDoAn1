package com.vn.backend.dto.response.assignment;

import com.vn.backend.enums.GradeRange;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScoreDistributionQueryDTO {
    private GradeRange range;
    private Long count;
}
