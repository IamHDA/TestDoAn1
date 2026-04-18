package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.request.submission.SubmissionSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionUpdateRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.submission.SubmissionDetailResponse;
import com.vn.backend.dto.response.submission.SubmissionSearchResponse;
import com.vn.backend.entities.Assignment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {

    SubmissionDetailResponse getDetailSubmission(String submissionId);

    SubmissionDetailResponse getMySubmission(String assignmentId);

    void createDefaultSubmissions(Assignment assignment);

    void addAttachmentToSubmission(String submissionId, SubmissionUpdateRequest request);

    void deleteAttachmentInSubmission(String attachmentId);

    ResponseListData<SubmissionSearchResponse> searchSubmission(BaseFilterSearchRequest<SubmissionSearchRequest> request);

    void markSubmission(String submissionId, SubmissionGradeUpdateRequest request);

    Resource downloadAllSubmissions(String assignmentId);

    ByteArrayResource downloadGradeTemplate(String assignmentId);

    void importSubmissionScoresFromExcel(String asignmentId, MultipartFile file);

}
