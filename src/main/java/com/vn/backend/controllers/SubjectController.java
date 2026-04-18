package com.vn.backend.controllers;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.subject.SubjectCreateRequest;
import com.vn.backend.dto.request.subject.SubjectSearchRequest;
import com.vn.backend.dto.request.subject.UpdateSubjectRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.subject.SubjectSearchResponse;
import com.vn.backend.services.SubjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConst.API + "/subjects")
public class SubjectController extends BaseController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping("/create")
    public AppResponse<Void> createSubject(
            @RequestBody @Valid SubjectCreateRequest request
    ) {
        log.info("Received request to create subject");
        subjectService.createSubject(request);
        log.info("Successfully created subject");
        return success(null);
    }

    @PostMapping("/search")
    public AppResponse<ResponseListData<SubjectSearchResponse>> searchSubject(
            @RequestBody @Valid BaseFilterSearchRequest<SubjectSearchRequest> request
    ) {
        log.info("Received request to search subject");
        ResponseListData<SubjectSearchResponse> responseListData = subjectService.searchSubject(request);
        log.info("Successfully search subject");
        return successListData(responseListData);
    }

    @PutMapping("/{subjectId}/update")
    public AppResponse<Void> updateSubject(
        @PathVariable Long subjectId,
        @RequestBody @Valid UpdateSubjectRequest request
    ) {
        log.info("Received request to update subject: {}", subjectId);
        subjectService.updateSubject(subjectId, request);
        log.info("Successfully updated subject: {}", subjectId);
        return success(null);
    }

    @DeleteMapping("/{subjectId}")
    public AppResponse<Void> deleteSubject(@PathVariable Long subjectId) {
        log.info("Received request to delete subject: {}", subjectId);
        subjectService.deleteSubject(subjectId);
        log.info("Successfully deleted subject: {}", subjectId);
        return success(null);
    }
}
