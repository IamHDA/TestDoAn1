package com.vn.backend.dto.request.common;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseFilterSearchRequest<T> {

    @Valid
    private T filters;

    @Valid
    private SearchRequest pagination;
}
