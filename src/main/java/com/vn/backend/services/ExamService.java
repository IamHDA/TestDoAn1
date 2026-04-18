package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.*;
import com.vn.backend.dto.request.question.QuestionAvailableSearchRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsSearchResponse;
import com.vn.backend.dto.response.exam.ExamResponse;
import com.vn.backend.dto.response.exam.ExamSearchResponse;
import com.vn.backend.dto.response.exam.ExamStatisticResponse;
import com.vn.backend.dto.response.question.QuestionAvailableSearchResponse;

import java.util.List;

public interface ExamService {

    void createExam(ExamCreateRequest request);

    ResponseListData<ExamSearchResponse> searchExam(BaseFilterSearchRequest<ExamSearchRequest> request);

    ExamResponse getExam(String examId);

    void updateExam(String examId, ExamUpdateRequest request);

    void deleteExam(String examId);

    void addQuestionsToExam(String examId, List<ExamQuestionsCreateRequest> request);

    void removeQuestionsFromExam(List<ExamQuestionsDeleteRequest> request);

    void updateQuestionInExam(ExamQuestionUpdateRequest request);

    ResponseListData<ExamQuestionsSearchResponse> searchExamQuestion(BaseFilterSearchRequest<ExamQuestionsSearchRequest> request);

    ExamStatisticResponse getExamStatistic(String examId);

    void duplicateExam(String examId);

    ResponseListData<QuestionAvailableSearchResponse> searchAvailableQuestions(BaseFilterSearchRequest<QuestionAvailableSearchRequest> request);
}
