package com.vn.backend.dto.response.approvalrequest;

import com.vn.backend.dto.response.classroom.ClassroomDetailResponse;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.entities.ApprovalRequestItems;
import com.vn.backend.enums.EntityType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalRequestItemResponse {

    private Long id;
    private Long requestId;
    private EntityType entityType;
    private Long entityId;
    private ClassroomDetailResponse classroom;
    private QuestionDetailResponse question;
    private TopicResponse topicResponse;

    public static ApprovalRequestItemResponse fromEntity(ApprovalRequestItems entity) {
        return ApprovalRequestItemResponse.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .build();
    }
}
