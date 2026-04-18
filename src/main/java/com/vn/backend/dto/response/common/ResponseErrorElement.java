package com.vn.backend.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ResponseErrorElement {

    private String field;
    private String code;
    private String message;
}
