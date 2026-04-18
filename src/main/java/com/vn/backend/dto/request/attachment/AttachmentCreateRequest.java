package com.vn.backend.dto.request.attachment;

import com.vn.backend.annotation.NotAllowBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentCreateRequest {
    @NotAllowBlank(message = "File URL is required")
    private String fileUrl;
    @NotAllowBlank(message = "File name is required")
    private String fileName;

    public AttachmentCreateRequestDTO toDTO() {
        return AttachmentCreateRequestDTO.builder()
                .fileName(this.fileName)
                .fileUrl(this.fileUrl)
                .build();
    }
}
