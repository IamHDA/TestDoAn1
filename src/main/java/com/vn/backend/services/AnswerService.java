package com.vn.backend.services;

import com.vn.backend.dto.request.answer.AnswerCreateRequest;
import com.vn.backend.dto.request.answer.AnswerUpdateRequest;
import com.vn.backend.dto.response.answer.AnswerResponse;
import java.util.List;

public interface AnswerService {
    AnswerResponse addAnswer(AnswerCreateRequest request);
    AnswerResponse updateAnswer(Long answerId, AnswerUpdateRequest request);
    void softDeleteAnswer(Long answerId);
    void reorderAnswers(Long questionId, List<Long> answerOrderIds);
}
