package com.vn.backend.dto.request.announcement;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
public class AnnouncementListRequest extends BaseFilterSearchRequest<AnnouncementFilterRequest> {
}
