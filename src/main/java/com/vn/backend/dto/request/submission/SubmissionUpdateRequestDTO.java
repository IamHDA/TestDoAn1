package com.vn.backend.dto.request.submission;

import com.vn.backend.dto.request.attachment.AttachmentCreateRequestDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubmissionUpdateRequestDTO {
    private List<AttachmentCreateRequestDTO> attachmentCreateRequestDTOList;
}
