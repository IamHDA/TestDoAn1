package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.ExamSaveAnswersRequest;
import com.vn.backend.dto.request.exam.ExamSubmissionRequest;
import com.vn.backend.dto.request.sessionexam.SaveAnswersRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamCreateRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchStudentRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchTeacherRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamUpdateRequest;
import com.vn.backend.dto.request.sessionexam.SubmitExamRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsResponse;
import com.vn.backend.dto.response.exam.ExamResultResponse;
import com.vn.backend.dto.response.exam.ExamSaveAnswersResponse;
import com.vn.backend.dto.response.exam.ExamSubmissionResponse;
import com.vn.backend.dto.response.exam.StudentExamOverviewResponse;
import com.vn.backend.dto.response.sessionexam.DownloadExamResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamMonitoringResponse;
import com.vn.backend.dto.response.sessionexam.JoinExamResponse;
import com.vn.backend.dto.response.sessionexam.SaveAnswersResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamDescriptiveStatisticResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamDetailResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamSearchStudentResponse;
import com.vn.backend.dto.response.sessionexam.SessionExamSearchTeacherResponse;
import com.vn.backend.dto.response.sessionexam.SubmitExamResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentExamResultResponse;
import com.vn.backend.services.SessionExamService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AppConst.API + "/session-exams")
public class SessionExamController extends BaseController {

  private final SessionExamService sessionExamService;

  public SessionExamController(SessionExamService sessionExamService) {
    this.sessionExamService = sessionExamService;
  }

  @PostMapping("/create")
  public AppResponse<SessionExamResponse> create(
      @RequestBody @Valid SessionExamCreateRequest request) {
    log.info("Received request to create session exam");
    SessionExamResponse response = sessionExamService.create(request);
    log.info("Successfully created session exam");
    return success(response);
  }

  @PutMapping("/update/{sessionExamId}")
  public AppResponse<SessionExamResponse> update(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId,
      @RequestBody @Valid SessionExamUpdateRequest request
  ) {
    log.info("Received request to update session exam {}", sessionExamId);
    SessionExamResponse response = sessionExamService.update(Long.valueOf(sessionExamId), request);
    log.info("Successfully updated session exam {}", sessionExamId);
    return success(response);
  }

  @GetMapping("/detail/{sessionExamId}")
  public AppResponse<SessionExamDetailResponse> getDetail(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId
  ) {
    log.info("Received request to get session exam detail {}", sessionExamId);
    SessionExamDetailResponse response = sessionExamService.getDetail(Long.valueOf(sessionExamId));
    log.info("Successfully got session exam detail {}", sessionExamId);
    return success(response);
  }

  @DeleteMapping("/delete/{sessionExamId}")
  public AppResponse<Void> delete(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId
  ) {
    log.info("Received request to delete session exam {}", sessionExamId);
    sessionExamService.delete(Long.valueOf(sessionExamId));
    log.info("Successfully deleted session exam {}", sessionExamId);
    return success(null);
  }

  @PostMapping("/teacher/search")
  public AppResponse<ResponseListData<SessionExamSearchTeacherResponse>> searchSessionExam(
      @Valid
      @RequestBody BaseFilterSearchRequest<SessionExamSearchTeacherRequest> request
  ) {
    log.info("Received request to search session exam by teacher");
    ResponseListData<SessionExamSearchTeacherResponse> response = sessionExamService.searchSessionExamByTeacher(
        request);
    log.info("Successfully search session exam by teacher");
    return successListData(response);
  }

  @PostMapping("/student/search")
  public AppResponse<ResponseListData<SessionExamSearchStudentResponse>> searchSessionExamByStudent(
      @Valid
      @RequestBody BaseFilterSearchRequest<SessionExamSearchStudentRequest> request
  ) {
    log.info("Received request to search session exam by student");
    ResponseListData<SessionExamSearchStudentResponse> response = sessionExamService.searchSessionExamByStudent(
        request);
    log.info("Successfully search session exam by student");
    return successListData(response);
  }

  @GetMapping("/exam-questions/{sessionExamId}")
  public AppResponse<ExamQuestionsResponse> getExamQuestions(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId) {
    log.info("Received request to get exam questions for session exam {}", sessionExamId);
    ExamQuestionsResponse response = sessionExamService.getExamQuestions(
        Long.valueOf(sessionExamId));
    log.info("Successfully got exam questions for session exam {}", sessionExamId);
    return success(response);
  }

  @PostMapping("/save-answers")
  public AppResponse<ExamSaveAnswersResponse> saveExamAnswers(
      @RequestBody @Valid ExamSaveAnswersRequest request) {
    log.info("Received request to save exam answers for session exam {}",
        request.getSessionExamId());
    ExamSaveAnswersResponse response = sessionExamService.saveExamAnswers(request);
    log.info("Successfully saved exam answers for session exam {}", request.getSessionExamId());
    return success(response);
  }

  @PostMapping("/submit-exam")
  public AppResponse<ExamSubmissionResponse> submitExam(
      @RequestBody @Valid ExamSubmissionRequest request) {
    log.info("Received request to submit exam for session exam {}", request.getSessionExamId());
    ExamSubmissionResponse response = sessionExamService.submitExam(request);
    log.info("Successfully submitted exam for session exam {}", request.getSessionExamId());
    return success(response);
  }

  @PostMapping("/results/{sessionExamId}")
  public AppResponse<ResponseListData<ExamResultResponse>> getExamResults(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId,
      @RequestBody @Valid BaseFilterSearchRequest<?> request) {
    log.info("Received request to get exam results for session exam {}", sessionExamId);
    ResponseListData<ExamResultResponse> response = sessionExamService.getExamResults(
        Long.valueOf(sessionExamId), request);
    log.info("Successfully got exam results for session exam {}", sessionExamId);
    return successListData(response);
  }

  @GetMapping("/student/overview/{sessionExamId}")
  public AppResponse<StudentExamOverviewResponse> getStudentExamOverview(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID_KEY, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId) {
    log.info("Received request to get student exam overview for session exam {}", sessionExamId);
    StudentExamOverviewResponse response = sessionExamService.getStudentExamOverview(
        Long.valueOf(sessionExamId));
    log.info("Successfully got student exam overview for session exam {}", sessionExamId);
    return success(response);
  }

  @PostMapping("/{sessionExamId}/join")
  public AppResponse<JoinExamResponse> joinSessionExam(
      @PathVariable Long sessionExamId) {
    log.info("Received request to join live session exam");
    JoinExamResponse response = sessionExamService.joinSessionExam(sessionExamId);
    log.info("Successfully joined live session exam");
    return success(response);
  }

  @GetMapping("/{sessionExamId}/download")
  public AppResponse<DownloadExamResponse> downloadExam(
      @PathVariable Long sessionExamId,
      @RequestHeader("X-Session-Token") String sessionToken) {
    log.info("Received request to download exam");
    DownloadExamResponse response = sessionExamService.downloadExam(
        sessionExamId, sessionToken);
    log.info("Successfully downloaded exam");
    return success(response);
  }

  @PostMapping("/{sessionExamId}/save")
  public AppResponse<SaveAnswersResponse> saveAnswers(
      @PathVariable Long sessionExamId,
      @RequestHeader("X-Session-Token") String sessionToken,
      @RequestBody @Valid SaveAnswersRequest request) {
    log.info("Received request to save answers");
    SaveAnswersResponse response = sessionExamService.saveAnswers(
        sessionExamId, sessionToken, request);
    log.info("Successfully saved answers");
    return success(response);
  }

  @PostMapping("/{sessionExamId}/submit")
  public AppResponse<SubmitExamResponse> submitExam(
      @PathVariable Long sessionExamId,
      @RequestHeader("X-Session-Token") String sessionToken,
      @RequestBody @Valid SubmitExamRequest request) {
    log.info("Received request to submit exam");
    SubmitExamResponse response = sessionExamService.submitExam(
        sessionExamId, sessionToken, request);
    log.info("Successfully submitted exam");
    return success(response);
  }

  @GetMapping("/{sessionExamId}/monitoring")
  public AppResponse<SessionExamMonitoringResponse> getExamMonitoring(
      @PathVariable Long sessionExamId) {
    log.info("Received request to get exam monitoring");
    SessionExamMonitoringResponse response = sessionExamService.getExamMonitoring(sessionExamId);
    log.info("Successfully got exam monitoring");
    return success(response);
  }

  @GetMapping("/{sessionExamId}/descriptive-statistic")
  public AppResponse<SessionExamDescriptiveStatisticResponse> getDescriptiveStatistic(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.SESSION_EXAM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
      String sessionExamId) {
    log.info("Received request to get descriptive statistic for session exam {}", sessionExamId);
    SessionExamDescriptiveStatisticResponse response = sessionExamService.getDescriptiveStatistic(
        Long.valueOf(sessionExamId));
    log.info("Successfully got descriptive statistic for session exam {}", sessionExamId);
    return success(response);
  }

  @GetMapping("/result/{studentSessionExamId}")
  public AppResponse<StudentExamResultResponse> getStudentExamResult(
      @PathVariable
      @AllowFormat(regex = RegexConst.INTEGER, fieldName = FieldConst.STUDENT_SESSION_EXAM_ID, message = MessageConst.INVALID_NUMBER_FORMAT)
      String studentSessionExamId
  ) {
    StudentExamResultResponse response = sessionExamService.getStudentExamResult(
        Long.parseLong(studentSessionExamId));
    return success(response);
  }
}


