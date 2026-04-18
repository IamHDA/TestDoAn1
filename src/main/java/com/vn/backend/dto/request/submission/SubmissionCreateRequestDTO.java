package com.vn.backend.dto.request.submission;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubmissionCreateRequestDTO {
    private Long assignmentId;
    private List<String> attachmentUrls;
}
