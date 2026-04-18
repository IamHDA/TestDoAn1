package com.vn.backend.services;

import com.vn.backend.dto.request.session.SubmitAnswerRequest;
import com.vn.backend.dto.response.session.NextQuestionResponse;
import com.vn.backend.dto.response.session.PracticeSetResponse;
import com.vn.backend.dto.response.session.SubmitAnswerResponse;

public interface AdaptiveSessionService {
    NextQuestionResponse getNextQuestion(Long subjectId);
    SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request);
    PracticeSetResponse getPracticeSet(Long topicId);
}

