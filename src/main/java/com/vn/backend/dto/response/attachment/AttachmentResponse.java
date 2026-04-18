package com.vn.backend.dto.response.attachment;

import com.vn.backend.entities.Attachment;
import com.vn.backend.utils.ModelMapperUtils;
import lombok.Builder;
import lombok.Data;

@Data
public class AttachmentResponse {

    private Long attachmentId;

    private String fileName;

    private String fileUrl;

    public static AttachmentResponse fromEntity(Attachment entity){
        return ModelMapperUtils.mapTo(entity, AttachmentResponse.class);
    }
}
