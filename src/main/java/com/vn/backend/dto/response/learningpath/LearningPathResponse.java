package com.vn.backend.dto.response.learningpath;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {
    private Long subjectId;
    private String subjectName;
    private List<TopicNode> topics;
    private List<PrerequisiteEdge> prerequisites;
    private List<UserMasteryStatus> userMasteryStatuses;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicNode {
        private Long topicId;
        private String topicName;
        private Long subjectId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrerequisiteEdge {
        private Long fromTopicId; // Prerequisite topic
        private Long toTopicId; // Topic that requires the prerequisite
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMasteryStatus {
        private Long topicId;
        private Double masteryScore;
        private MasteryState state; // LOCKED, LEARNING, MASTERED
        
        public enum MasteryState {
            LOCKED,     // Chưa đủ prerequisites
            LEARNING,   // Đang học (0 < masteryScore < 0.8)
            MASTERED    // Đã master (masteryScore >= 0.8)
        }
    }
}

