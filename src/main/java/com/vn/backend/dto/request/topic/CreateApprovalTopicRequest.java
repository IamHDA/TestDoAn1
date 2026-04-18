package com.vn.backend.dto.request.topic;

import com.vn.backend.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class CreateApprovalTopicRequest {
    @NotNull(message = "Request type is required")
    private RequestType requestType;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Request Description is required")
    private String requestDescription;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    private List<CreateTopicRequest> topicRequests;

}
