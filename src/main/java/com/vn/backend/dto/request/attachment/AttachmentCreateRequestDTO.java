package com.vn.backend.dto.request.attachment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentCreateRequestDTO {
    private String fileUrl;
    private String fileName;
}
