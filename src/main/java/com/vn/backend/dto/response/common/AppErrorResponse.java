package com.vn.backend.dto.response.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class AppErrorResponse<T> extends AppResponse<T> {

    private boolean success;
    private Object errors;

    public AppErrorResponse(AppResponse<T> appResponse, Object errors) {
        super(appResponse.isSuccess(), appResponse.getData(), null);
        this.errors = errors;
    }
}
