package com.vn.backend.dto.response.assignment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreDistributionResponse {
    private String range;
    private Long count;

    public static ScoreDistributionResponse fromDTO(ScoreDistributionQueryDTO dto) {
        String rangeLabel = dto.getRange().getMin() + " - " + dto.getRange().getMax();
        return ScoreDistributionResponse.builder()
                .range(rangeLabel)
                .count(dto.getCount())
                .build();
    }
}
