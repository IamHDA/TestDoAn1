package com.vn.backend.controllers;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.ExamCreateRequest;
import com.vn.backend.dto.request.exam.ExamQuestionUpdateRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsCreateRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsDeleteRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsSearchRequest;
import com.vn.backend.dto.request.exam.ExamSearchRequest;
import com.vn.backend.dto.request.exam.ExamUpdateRequest;
import com.vn.backend.dto.request.question.QuestionAvailableSearchRequest;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsSearchResponse;
import com.vn.backend.dto.response.exam.ExamResponse;
import com.vn.backend.dto.response.exam.ExamSearchResponse;
import com.vn.backend.dto.response.exam.ExamStatisticResponse;
import com.vn.backend.dto.response.question.QuestionAvailableSearchResponse;
import com.vn.backend.services.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(AppConst.API + "/exams")
public class ExamController extends BaseController {

  private final ExamService examService;

  public ExamController(ExamService examService) {
    this.examService = examService;
  }

  @PostMapping("/create")
  public AppResponse<Void> createExam(
      @RequestBody
      @Valid
      ExamCreateRequest request
  ) {
    log.info("Received request to create exam");
    examService.createExam(request);
    log.info("Successfully created exam");
    return success(null);
  }


  @PostMapping("/search")
  public AppResponse<ResponseListData<ExamSearchResponse>> searchExam(
      @Valid @RequestBody BaseFilterSearchRequest<ExamSearchRequest> request
  ) {
    log.info("Received request to search exams");
    ResponseListData<ExamSearchResponse> response = examService.searchExam(request);
    log.info("Successfully searched exams");
    return success(response);
  }

  @GetMapping("/{examId}")
  public AppResponse<ExamResponse> getExam(@PathVariable String examId) {
    log.info("Received request to get exam detail: {}", examId);
    ExamResponse response = examService.getExam(examId);
    log.info("Successfully retrieved exam detail: {}", examId);
    return success(response);
  }

  @PutMapping("/{examId}")
  public AppResponse<Void> updateExam(
      @PathVariable String examId,
      @Valid @RequestBody ExamUpdateRequest request
  ) {
    log.info("Received request to update exam: {}", examId);
    examService.updateExam(examId, request);
    log.info("Successfully updated exam: {}", examId);
    return success(null);
  }

  @DeleteMapping("/{examId}")
  public AppResponse<Void> deleteExam(@PathVariable String examId) {
    log.info("Received request to delete exam: {}", examId);
    examService.deleteExam(examId);
    log.info("Successfully deleted exam: {}", examId);
    return success(null);
  }

  @PostMapping("/{examId}/questions/create")
  public AppResponse<Void> createQuestionsToExam(
      @PathVariable
      @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
      @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
      String examId,
      @Valid @RequestBody List<ExamQuestionsCreateRequest> request
  ) {
    log.info("Received request to add questions to exam");
    examService.addQuestionsToExam(examId, request);
    log.info("Successfully added questions to exam");
    return success(null);
  }

  @PostMapping("/questions/search")
  public AppResponse<ResponseListData<ExamQuestionsSearchResponse>> searchExamQuestions(
      @Valid @RequestBody BaseFilterSearchRequest<ExamQuestionsSearchRequest> request
  ) {
    log.info("Received request to search questions in exam");
    ResponseListData<ExamQuestionsSearchResponse> response = examService.searchExamQuestion(
        request);
    log.info("Successfully searched exam questions");
    return successListData(response);
  }

  @PostMapping("/{examId}/duplicate")
  public AppResponse<Void> duplicateExam(
      @PathVariable
      @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
      @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
      String examId
  ) {
    log.info("Received request to duplicate exam");
    examService.duplicateExam(examId);
    log.info("Successfully duplicate exam");
    return success(null);
  }

  @GetMapping("/{examId}/statistic")
  public AppResponse<ExamStatisticResponse> getExamStatistic(
      @PathVariable
      @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.EXAM_ID)
      @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.EXAM_ID)
      String examId
  ) {
    log.info("Received request to get exam statistic");
    ExamStatisticResponse response = examService.getExamStatistic(examId);
    log.info("Successfully get exam statistic");
    return success(response);
  }

  @PostMapping("/available-questions/search")
  public AppResponse<ResponseListData<QuestionAvailableSearchResponse>> searchAvailableQuestions(
      @Valid @RequestBody BaseFilterSearchRequest<QuestionAvailableSearchRequest> request
  ) {
    log.info("Received request to search available questions");
    ResponseListData<QuestionAvailableSearchResponse> response = examService.searchAvailableQuestions(
        request);
    log.info("Successfully search available questions");
    return successListData(response);
  }
}
