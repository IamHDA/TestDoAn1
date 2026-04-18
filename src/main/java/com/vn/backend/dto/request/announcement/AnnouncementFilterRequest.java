package com.vn.backend.dto.request.announcement;

import com.vn.backend.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementFilterRequest {
    private AnnouncementType type;
}
