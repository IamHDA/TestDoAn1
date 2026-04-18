package com.vn.backend.dto.response.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"content", "paging"})
public class ResponseListData<D> {

    /**
     * Response data
     */
    private Collection<D> content;

    /**
     * Paging Handle Ignore on result if null
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PagingMeta paging;
}
