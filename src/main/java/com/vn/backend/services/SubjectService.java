package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.subject.SubjectCreateRequest;
import com.vn.backend.dto.request.subject.SubjectSearchRequest;
import com.vn.backend.dto.request.subject.UpdateSubjectRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.subject.SubjectSearchResponse;

public interface SubjectService {

    void createSubject(SubjectCreateRequest request);

    ResponseListData<SubjectSearchResponse> searchSubject(BaseFilterSearchRequest<SubjectSearchRequest> request);

    void updateSubject(Long subjectId, UpdateSubjectRequest request);
    void deleteSubject(Long subjectId);
}
