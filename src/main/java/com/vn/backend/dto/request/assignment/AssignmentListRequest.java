package com.vn.backend.dto.request.assignment;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentListRequest extends BaseFilterSearchRequest<AssignmentFilterRequest> {
}
