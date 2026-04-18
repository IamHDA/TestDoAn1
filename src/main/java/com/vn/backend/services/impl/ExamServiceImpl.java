package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.ExamCreateRequest;
import com.vn.backend.dto.request.exam.ExamCreateRequestDTO;
import com.vn.backend.dto.request.exam.ExamQuestionUpdateRequest;
import com.vn.backend.dto.request.exam.ExamQuestionUpdateRequestDTO;
import com.vn.backend.dto.request.exam.ExamQuestionsCreateRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsCreateRequestDTO;
import com.vn.backend.dto.request.exam.ExamQuestionsDeleteRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsDeleteRequestDTO;
import com.vn.backend.dto.request.exam.ExamQuestionsSearchRequest;
import com.vn.backend.dto.request.exam.ExamQuestionsSearchRequestDTO;
import com.vn.backend.dto.request.exam.ExamSearchRequest;
import com.vn.backend.dto.request.exam.ExamSearchRequestDTO;
import com.vn.backend.dto.request.exam.ExamUpdateRequest;
import com.vn.backend.dto.request.exam.ExamUpdateRequestDTO;
import com.vn.backend.dto.request.question.QuestionAvailableSearchRequest;
import com.vn.backend.dto.request.question.QuestionAvailableSearchRequestDTO;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.DifficultyDistributionResponse;
import com.vn.backend.dto.response.exam.ExamQuestionsSearchResponse;
import com.vn.backend.dto.response.exam.ExamResponse;
import com.vn.backend.dto.response.exam.ExamSearchResponse;
import com.vn.backend.dto.response.exam.ExamStatisticResponse;
import com.vn.backend.dto.response.exam.TopicDistributionResponse;
import com.vn.backend.dto.response.question.QuestionAvailableSearchResponse;
import com.vn.backend.entities.Exam;
import com.vn.backend.entities.ExamQuestion;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.User;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ExamQuestionRepository;
import com.vn.backend.repositories.ExamRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.ExamService;
import com.vn.backend.utils.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamServiceImpl extends BaseService implements ExamService {

  private final AuthService authService;
  private final ExamRepository examRepository;
  private final SubjectRepository subjectRepository;
  private final QuestionRepository questionRepository;
  private final ExamQuestionRepository examQuestionRepository;

  public ExamServiceImpl(MessageUtils messageUtils, AuthService authService,
      ExamRepository examRepository, SubjectRepository subjectRepository,
      QuestionRepository questionRepository, ExamQuestionRepository examQuestionRepository) {
    super(messageUtils);
    this.authService = authService;
    this.examRepository = examRepository;
    this.subjectRepository = subjectRepository;
    this.questionRepository = questionRepository;
    this.examQuestionRepository = examQuestionRepository;
  }

  @Override
  public void createExam(ExamCreateRequest request) {
    log.info("Start service to create exam");

    User user = authService.getCurrentUser();
    ExamCreateRequestDTO dto = request.toDTO();
    dto.setCreatedBy(user.getId());

    if (!subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(dto.getSubjectId())) {
      throw new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }

    Exam exam = dto.toEntity();
    examRepository.save(exam);
    log.info("End service create exam");
  }

  @Override
  public ResponseListData<ExamSearchResponse> searchExam(
      BaseFilterSearchRequest<ExamSearchRequest> request) {
    log.info("Start service to search exam");

    User user = authService.getCurrentUser();
    ExamSearchRequestDTO dto = request.getFilters().toDTO();
    dto.setCreatedBy(user.getId());

    Pageable pageable = request.getPagination().getPagingMeta().toPageable();
    Page<Exam> examPage = examRepository.searchExam(dto, pageable);
    List<ExamSearchResponse> response = examPage.stream()
        .map(ExamSearchResponse::fromEntity)
        .toList();
    PagingMeta pagingMeta = request.getPagination().getPagingMeta();
    pagingMeta.setTotalRows(examPage.getTotalElements());
    pagingMeta.setTotalPages(examPage.getTotalPages());

    log.info("End service search exam");
    return new ResponseListData<>(response, pagingMeta);
  }

  @Override
  public ExamResponse getExam(String examId) {
    log.info("Start service to get exam");

    User user = authService.getCurrentUser();
    Exam exam = examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(Long.parseLong(examId),
        user.getId()).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    log.info("End service get exam");
    return ExamResponse.fromEntity(exam);
  }

  @Override
  public void updateExam(String examId, ExamUpdateRequest request) {
    log.info("Start service to update exam");

    User user = authService.getCurrentUser();
    ExamUpdateRequestDTO dto = request.toDTO();

    Exam exam = examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(Long.parseLong(examId),
        user.getId()).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    if (!subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(dto.getSubjectId())) {
      throw new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }

    exam.setSubjectId(dto.getSubjectId());
    exam.setTitle(dto.getTitle());
    exam.setDescription(dto.getDescription());

    log.info("End service update exam");
    examRepository.save(exam);
  }

  @Override
  public void deleteExam(String examId) {
    log.info("Start service to delete exam");

    User user = authService.getCurrentUser();
    Exam exam = examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(Long.parseLong(examId),
        user.getId()).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
    exam.setIsDeleted(true);
    examRepository.save(exam);

    log.info("End service delete exam");
  }

  @Override
  @Transactional
  public void addQuestionsToExam(String examId, List<ExamQuestionsCreateRequest> request) {
    log.info("Start service to add questions to exam");

    User user = authService.getCurrentUser();
    Map<Long, ExamQuestionsCreateRequestDTO> dtoMap = request.stream()
        .map(ExamQuestionsCreateRequest::toDTO)
        .collect(Collectors.toMap(
            ExamQuestionsCreateRequestDTO::getQuestionId,
            Function.identity()
        ));
    Exam exam = examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(
        Long.parseLong(examId),
        user.getId()).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
    List<ExamQuestion> examQuestions = examQuestionRepository.getAllExamQuestion(exam.getExamId(),
        user.getId());

    List<ExamQuestion> toUpdate = new ArrayList<>();
    List<ExamQuestion> toInsert = new ArrayList<>();

    for (ExamQuestion entity : examQuestions) {
      if (!dtoMap.containsKey(entity.getQuestionId())) {
        examQuestionRepository.delete(entity);
        continue;
      }
      entity.setOrderIndex(dtoMap.get(entity.getQuestionId()).getOrderIndex());
      toUpdate.add(entity);
      dtoMap.remove(entity.getQuestionId());
    }

    for (Map.Entry<Long, ExamQuestionsCreateRequestDTO> entry : dtoMap.entrySet()) {
      Question question = questionRepository.findByQuestionIdAndCreatedByAndIsDeletedFalse(
          entry.getKey(), user.getId()
      ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
      ExamQuestion examQuestion = ExamQuestion.builder()
          .examId(exam.getExamId())
          .questionId(question.getQuestionId())
          .orderIndex(entry.getValue().getOrderIndex())
          .build();
      toInsert.add(examQuestion);
    }

    if (!toUpdate.isEmpty()) {
      examQuestionRepository.saveAll(toUpdate);
    }
    if (!toInsert.isEmpty()) {
      examQuestionRepository.saveAll(toInsert);
    }

    log.info("End service add questions to exam");
  }

  @Override
  @Transactional
  public void removeQuestionsFromExam(List<ExamQuestionsDeleteRequest> request) {
    log.info("Start service to remove questions from exam");

    User user = authService.getCurrentUser();
    List<ExamQuestionsDeleteRequestDTO> dtoList = request.stream()
        .map(ExamQuestionsDeleteRequest::toDTO)
        .toList();
    for (ExamQuestionsDeleteRequestDTO dto : dtoList) {
      ExamQuestion examQuestion = examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(
          dto.getExamId(), dto.getQuestionId(), user.getId()
      ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
      examQuestionRepository.delete(examQuestion);
    }

    log.info("End service remove questions from exam");
  }

  @Override
  public void updateQuestionInExam(ExamQuestionUpdateRequest request) {
    log.info("Start service to update question in exam");

    User user = authService.getCurrentUser();
    ExamQuestionUpdateRequestDTO dto = request.toDTO();
    ExamQuestion examQuestion = examQuestionRepository.findByExamIdAndQuestionIdAndExam_CreatedBy(
        dto.getExamId(), dto.getQuestionId(), user.getId()
    ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
    examQuestion.setOrderIndex(dto.getOrderIndex());
    examQuestion.setScore(dto.getScore());

    log.info("End service update question in exam");
    examQuestionRepository.save(examQuestion);
  }

  @Override
  public ResponseListData<ExamQuestionsSearchResponse> searchExamQuestion(
      BaseFilterSearchRequest<ExamQuestionsSearchRequest> request) {
    log.info("Start service to search exam question");

    User user = authService.getCurrentUser();
    ExamQuestionsSearchRequestDTO dto = request.getFilters().toDTO();
    dto.setCreatedBy(user.getId());
    Pageable pageable = request.getPagination().getPagingMeta().toPageable();
    Page<ExamQuestion> queryDTOS = examQuestionRepository.searchExamQuestion(dto, pageable);
    List<ExamQuestionsSearchResponse> response = queryDTOS.stream()
        .map(ExamQuestionsSearchResponse::fromEntity)
        .toList();
    PagingMeta pagingMeta = request.getPagination().getPagingMeta();
    pagingMeta.setTotalRows(queryDTOS.getTotalElements());
    pagingMeta.setTotalPages(queryDTOS.getTotalPages());

    log.info("End service search exam question");
    return new ResponseListData<>(response, pagingMeta);
  }

  @Override
  @Transactional
  public void duplicateExam(String examId) {
    log.info("Start service to duplicate exam");

    User user = authService.getCurrentUser();
    Exam exam = examRepository.findByExamIdAndCreatedByAndIsDeletedIsFalse(
        Long.parseLong(examId), user.getId()
    ).orElseThrow(() -> new AppException(MessageConst.NOT_FOUND,
        messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    Exam newExam = Exam.builder()
        .subjectId(exam.getSubjectId())
        .title(exam.getTitle() + AppConst.COPY)
        .description(exam.getDescription() + AppConst.COPY)
        .createdBy(user.getId())
        .isDeleted(false)
        .build();

    examRepository.save(newExam);

    List<ExamQuestion> examQuestionList = examQuestionRepository.getAllExamQuestion(
        Long.parseLong(examId), user.getId());

    List<ExamQuestion> newExamQuestions = examQuestionList.stream()
        .map(eq -> ExamQuestion.builder()
            .examId(newExam.getExamId())
            .questionId(eq.getQuestionId())
            .orderIndex(eq.getOrderIndex())
            .score(eq.getScore())
            .build())
        .toList();

    log.info("End service duplicate exam");
    examQuestionRepository.saveAll(newExamQuestions);
  }

  @Override
  public ExamStatisticResponse getExamStatistic(String examId) {
    log.info("Start service to get exam statistic");

    User user = authService.getCurrentUser();
    Long examIdL = Long.parseLong(examId);
    long total = examQuestionRepository.countQuestions(examIdL, user.getId());

    List<DifficultyDistributionResponse> difficulty = examQuestionRepository.countByDifficulty(
        examIdL, user.getId());

    List<TopicDistributionResponse> topics = examQuestionRepository.countByTopic(
        examIdL, user.getId());

    log.info("End service get exam statistic");
    return ExamStatisticResponse.builder()
        .totalQuestion((int) total)
        .difficultyDistribution(difficulty)
        .topicDistribution(topics)
        .build();
  }

  @Override
  public ResponseListData<QuestionAvailableSearchResponse> searchAvailableQuestions(
      BaseFilterSearchRequest<QuestionAvailableSearchRequest> request) {
    log.info("Start service to search available questions");

    User user = authService.getCurrentUser();
    QuestionAvailableSearchRequestDTO dto = request.getFilters().toDTO();
    dto.setCreatedBy(user.getId());
    Pageable pageable = request.getPagination().getPagingMeta().toPageable();
    Page<Question> queryDTOS = questionRepository.searchAvailableQuestions(
        dto, pageable);
    Set<Long> addedQuestionIds = examQuestionRepository
        .findAllIdsByExamId(dto.getExamId());
    List<QuestionAvailableSearchResponse> response = queryDTOS.stream()
        .map(e -> QuestionAvailableSearchResponse.fromEntity(e, addedQuestionIds))
        .toList();
    PagingMeta pagingMeta = request.getPagination().getPagingMeta();
    pagingMeta.setTotalRows(queryDTOS.getTotalElements());
    pagingMeta.setTotalPages(queryDTOS.getTotalPages());

    log.info("End service search available questions");
    return new ResponseListData<>(response, pagingMeta);
  }
}
