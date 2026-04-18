package com.vn.backend.services;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.ExamSaveAnswersRequest;
import com.vn.backend.dto.request.exam.ExamSubmissionRequest;
import com.vn.backend.dto.request.sessionexam.*;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.*;
import com.vn.backend.dto.response.sessionexam.*;
import com.vn.backend.dto.response.studentsessionexam.StudentExamResultResponse;
import com.vn.backend.entities.SessionExam;

import java.time.LocalDateTime;

public interface SessionExamService {

    SessionExamResponse create(SessionExamCreateRequest request);

    SessionExamResponse update(Long sessionExamId, SessionExamUpdateRequest request);

    void delete(Long sessionExamId);

    SessionExamDetailResponse getDetail(Long sessionExamId);

    ResponseListData<SessionExamSearchTeacherResponse> searchSessionExamByTeacher(
            BaseFilterSearchRequest<SessionExamSearchTeacherRequest> request);

    ResponseListData<SessionExamSearchStudentResponse> searchSessionExamByStudent(
            BaseFilterSearchRequest<SessionExamSearchStudentRequest> request);

    ExamQuestionsResponse getExamQuestions(Long sessionExamId);

    ExamSaveAnswersResponse saveExamAnswers(ExamSaveAnswersRequest request);

    ExamSubmissionResponse submitExam(ExamSubmissionRequest request);

    ResponseListData<ExamResultResponse> getExamResults(Long sessionExamId,
                                                        BaseFilterSearchRequest<?> request);

    StudentExamOverviewResponse getStudentExamOverview(Long sessionExamId);

    // live mode
    void startLiveSessionExam(SessionExam sessionExam);

    JoinExamResponse joinSessionExam(Long sessionExamId);

    DownloadExamResponse downloadExam(Long sessionExamId, String sessionToken);

    SaveAnswersResponse saveAnswers(Long sessionExamId, String sessionToken,
                                    SaveAnswersRequest request);

    SubmitExamResponse submitExam(Long sessionExamId, String sessionToken, SubmitExamRequest request);

    void endLiveSessionExam(SessionExam sessionExam);

    SessionExamMonitoringResponse getExamMonitoring(Long sessionExamId);

    SessionExamDescriptiveStatisticResponse getDescriptiveStatistic(Long sessionExamId);

    StudentExamResultResponse getStudentExamResult(Long studentSessionExamId);

    void processFlexExamStarted(LocalDateTime now);

    void processExpiredNotStartedFlexExams(LocalDateTime now);
}


