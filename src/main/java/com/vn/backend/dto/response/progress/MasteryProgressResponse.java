package com.vn.backend.dto.response.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasteryProgressResponse {
    private List<TopicMastery> topicMasteries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicMastery {
        private Long topicId;
        private String topicName;
        private Long subjectId;
        private String subjectName;
        private Double masteryScore;
        private Integer totalAttempts;
        private Integer correctAttempts;
    }
}

