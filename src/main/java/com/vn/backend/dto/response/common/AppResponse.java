package com.vn.backend.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppResponse<T> {

    private boolean success;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PagingMeta paging;
}