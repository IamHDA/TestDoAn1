package com.vn.backend.services;

import com.vn.backend.dto.request.question.*;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.question.QuestionSearchResponse;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface QuestionService {
    QuestionDetailResponse createQuestion(QuestionCreateRequest request);

    List<QuestionDetailResponse> createQuestions(List<QuestionBulkCreateItemRequest> requests);

    QuestionDetailResponse getQuestionDetail(Long questionId);

    QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request);

    void softDeleteQuestion(Long questionId);

    ResponseListData<QuestionSearchResponse> searchQuestions(BaseFilterSearchRequest<QuestionSearchRequest> request);

    QuestionBulkCreateRequest importQuestionsFromExcel(MultipartFile file, QuestionImportExcelRequest request);

    byte[] exportQuestionsToExcel(QuestionSearchRequest request);

    byte[] downloadImportTemplate();

    void createApprovalQuestion(CreateApprovalQuestionRequest request);
}
