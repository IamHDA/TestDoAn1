package com.vn.backend.dto.request.subject;

import com.vn.backend.utils.SearchUtils;
import lombok.Data;

@Data
public class SubjectSearchRequest {

    private String keyword;

    public SubjectSearchRequestDTO toDTO(){
        return SubjectSearchRequestDTO.builder()
                .subjectCode(SearchUtils.getLikeValue(this.keyword))
                .subjectName(SearchUtils.getLikeValue(this.keyword))
                .build();
    }
}
