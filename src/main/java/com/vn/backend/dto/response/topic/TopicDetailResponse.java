package com.vn.backend.dto.response.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDetailResponse {
    private Long topicId;
    private String topicName;
    private Long subjectId;
    private String subjectName;
    
    private PrerequisiteTopicResponse prerequisiteTopic;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrerequisiteTopicResponse {
        private Long topicId;
        private String topicName;
    }
}


