package com.vn.backend.dto.request.submission;

import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.attachment.AttachmentCreateRequest;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SubmissionCreateRequest {

    @NotAllowBlank(fieldName = FieldConst.ASSIGNMENT_ID, message = MessageConst.REQUIRED_FIELD_EMPTY)
    private String assignmentId;

    @Valid
    private List<AttachmentCreateRequest> attachmentCreateRequestList;

    public SubmissionCreateRequestDTO toDTO() {
        return SubmissionCreateRequestDTO.builder()
                .assignmentId(Long.parseLong(assignmentId))
                .attachmentUrls(attachmentCreateRequestList.stream().map(AttachmentCreateRequest::getFileUrl).toList())
                .build();
    }
}
