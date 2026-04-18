package com.vn.backend.dto.request.comment;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CommentListRequest extends BaseFilterSearchRequest<CommentFilterRequest> {

    private Long announcementId;
}
