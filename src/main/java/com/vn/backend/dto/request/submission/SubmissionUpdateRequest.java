package com.vn.backend.dto.request.submission;

import com.vn.backend.dto.request.attachment.AttachmentCreateRequest;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SubmissionUpdateRequest {
    @Valid
    private List<AttachmentCreateRequest> attachmentCreateRequestList;

    public SubmissionUpdateRequestDTO toDTO() {
        return SubmissionUpdateRequestDTO.builder()
                .attachmentCreateRequestDTOList(
                        this.attachmentCreateRequestList.stream()
                                .map(AttachmentCreateRequest::toDTO)
                                .toList()
                )
                .build();
    }
}
