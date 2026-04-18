package com.vn.backend.dto.request.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicFilterRequest {
    
    private String keyword;
    private Long subjectId;
}
