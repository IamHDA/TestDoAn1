package com.vn.backend.dto.response.submission;

import com.vn.backend.dto.response.attachment.AttachmentResponse;
import com.vn.backend.dto.response.user.UserInfoResponse;
import com.vn.backend.entities.Attachment;
import com.vn.backend.entities.Submission;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubmissionDetailResponse {
    private Long submissionId;
    private UserInfoResponse userInfoResponse;
    private Double grade;
    private String gradingStatus;
    private String submissionStatus;
    private List<AttachmentResponse> attachmentResponseList;

    public static SubmissionDetailResponse fromEntity(Submission entity, List<Attachment> attachments) {
        return SubmissionDetailResponse.builder()
                .submissionId(entity.getSubmissionId())
                .userInfoResponse(ModelMapperUtils.mapTo(entity.getStudent(), UserInfoResponse.class))
                .grade(entity.getGrade())
                .gradingStatus(entity.getGradingStatus().toString())
                .submissionStatus(entity.getSubmissionStatus().toString())
                .attachmentResponseList(
                        attachments.stream()
                                .map(AttachmentResponse::fromEntity)
                                .toList()
                )
                .build();
    }
}
