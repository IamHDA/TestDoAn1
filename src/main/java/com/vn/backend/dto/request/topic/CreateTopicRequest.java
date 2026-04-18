package com.vn.backend.dto.request.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicRequest {

    @NotBlank(message = "Topic name is required")
    @Size(min = 2, max = 255, message = "Topic name must be between 2 and 255 characters")
    private String topicName;

    private Long topicId;

    private Long prerequisiteTopicId;
}

