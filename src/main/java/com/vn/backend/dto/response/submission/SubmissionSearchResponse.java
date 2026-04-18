package com.vn.backend.dto.response.submission;

import com.vn.backend.utils.ModelMapperUtils;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionSearchResponse {
    private Long submissionId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String fullName;

    private String submissionStatus;
    private String gradingStatus;
    private LocalDateTime submittedAt;
    private Double grade;

    public static SubmissionSearchResponse fromDTO(SubmissionSearchQueryDTO dto) {
        return ModelMapperUtils.mapTo(dto, SubmissionSearchResponse.class);
    }
}
