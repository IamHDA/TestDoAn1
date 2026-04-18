package com.vn.backend.services.impl;

import static com.vn.backend.constants.AppConst.TITLE_ANNOUCEMENT_MAP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.redis.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.exam.ExamSaveAnswersRequest;
import com.vn.backend.dto.request.exam.ExamSubmissionRequest;
import com.vn.backend.dto.request.sessionexam.SaveAnswersRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamCreateRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchStudentRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchStudentRequestDTO;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchTeacherRequest;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchTeacherRequestDTO;
import com.vn.backend.dto.request.sessionexam.SessionExamUpdateRequest;
import com.vn.backend.dto.request.sessionexam.StudentAnswerRequest;
import com.vn.backend.dto.request.sessionexam.SubmitExamRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.exam.ExamQuestionsResponse;
import com.vn.backend.dto.response.exam.ExamResultResponse;
import com.vn.backend.dto.response.exam.ExamSaveAnswersResponse;
import com.vn.backend.dto.response.exam.ExamSubmissionResponse;
import com.vn.backend.dto.response.exam.StudentExamOverviewResponse;
import com.vn.backend.dto.response.examquestionsnapshot.ExamQuestionSnapshotResponse;
import com.vn.backend.dto.response.sessionexam.*;
import com.vn.backend.dto.response.studentsessionexam.StudentExamQuestionResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentExamResultQueryDTO;
import com.vn.backend.dto.response.studentsessionexam.StudentExamResultResponse;
import com.vn.backend.entities.Announcement;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.Exam;
import com.vn.backend.entities.ExamQuestionAnswerSnapshot;
import com.vn.backend.entities.ExamQuestionSnapshot;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.SessionExamMonitoringLog;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.AnnouncementType;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.ExamSubmissionStatus;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.enums.QuestionType;
import com.vn.backend.enums.SessionExamPhase;
import com.vn.backend.enums.SessionExamStatus;
import com.vn.backend.enums.StudentExamStatus;
import com.vn.backend.enums.TitleAnnouncementType;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnnouncementRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.ExamQuestionSnapshotRepository;
import com.vn.backend.repositories.ExamRepository;
import com.vn.backend.repositories.SessionExamMonitoringLogRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.ExamQuestionSnapshotService;
import com.vn.backend.services.RedisService;
import com.vn.backend.services.SessionExamService;
import com.vn.backend.services.WebSocketService;
import com.vn.backend.utils.JsonUtils;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.RedisUtils;
import com.vn.backend.utils.SearchUtils;
import com.vn.backend.utils.Utils;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionExamServiceImpl extends BaseService implements SessionExamService {

  private final SessionExamRepository sessionExamRepository;
  private final AuthService authService;
  private final ExamRepository examRepository;
  private final UserRepository userRepository;
  private final SessionExamMonitoringLogRepository sessionExamMonitoringLogRepository;
  private final AnnouncementRepository announcementRepository;
  private final ClassroomRepository classroomRepository;
  private final ClassMemberRepository classMemberRepository;
  private final ExamQuestionSnapshotService examQuestionSnapshotService;
  private final StudentSessionExamRepository studentSessionExamRepository;
  private final ExamQuestionSnapshotRepository examQuestionSnapshotRepository;
  private final RedisService redisService;
  private final ObjectMapper objectMapper;
  private final WebSocketService webSocketService;

  public SessionExamServiceImpl(MessageUtils messageUtils,
      SessionExamRepository sessionExamRepository, AuthService authService,
      ExamRepository examRepository, UserRepository userRepository,
      SessionExamMonitoringLogRepository sessionExamMonitoringLogRepository,
      AnnouncementRepository announcementRepository,
      ClassroomRepository classroomRepository, ClassMemberRepository classMemberRepository,
      ExamQuestionSnapshotService examQuestionSnapshotService,
      StudentSessionExamRepository studentSessionExamRepository,
      ExamQuestionSnapshotRepository examQuestionSnapshotRepository,
      RedisService redisService,
      ObjectMapper objectMapper,
      WebSocketService webSocketService) {
    super(messageUtils);
    this.sessionExamRepository = sessionExamRepository;
    this.authService = authService;
    this.examRepository = examRepository;
    this.userRepository = userRepository;
    this.sessionExamMonitoringLogRepository = sessionExamMonitoringLogRepository;
    this.announcementRepository = announcementRepository;
    this.classroomRepository = classroomRepository;
    this.classMemberRepository = classMemberRepository;
    this.examQuestionSnapshotService = examQuestionSnapshotService;
    this.studentSessionExamRepository = studentSessionExamRepository;
    this.examQuestionSnapshotRepository = examQuestionSnapshotRepository;
    this.objectMapper = objectMapper;
    this.redisService = redisService;
    this.webSocketService = webSocketService;
  }

  @Override
  @Transactional
  public SessionExamResponse create(SessionExamCreateRequest request) {
    validateTimeForCreate(request.getStartDate(), request.getEndDate(), request.getDuration());
    User currentUser = authService.getCurrentUser();
    if (!classroomRepository.existsByClassroomIdAndTeacherId(request.getClassId(),
        currentUser.getId())) {
      throw new AppException(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN),
          messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
    }
    Classroom classroom = classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(request.getClassId(),
        currentUser.getId()).get();
    SessionExam entity = SessionExam.builder()
        .classId(request.getClassId())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .title(request.getTitle())
        .description(request.getDescription())
        .duration(request.getDuration())
        .examMode(request.getExamMode())
        .questionOrderMode(request.getQuestionOrderMode())
        .isInstantlyResult(Boolean.TRUE.equals(request.getIsInstantlyResult()))
        .createdBy(currentUser.getId())
        .examId(request.getExamId())
        .isDeleted(false)
        .build();
    SessionExam saved = sessionExamRepository.saveAndFlush(entity);

    examQuestionSnapshotService.createExamQuestionSnapshots(saved.getExamId(),
        saved.getSessionExamId());

    // Tạo announcement
    Announcement announcement = Announcement.builder()
        .classroomId(request.getClassId())
        .title(String.format(TITLE_ANNOUCEMENT_MAP.get(TitleAnnouncementType.EXAM),
            authService.getCurrentUser().getFullName(), classroom.getClassName()))
        .content(null)
        .type(AnnouncementType.EXAM)
        .allowComments(false)
        .createdByUser(currentUser)
        .createdBy(currentUser.getId())
        .isDeleted(false)
        .objectId(saved.getSessionExamId())
        .build();

    announcementRepository.save(announcement);
    return toResponse(saved);
  }

  @Override
  public SessionExamResponse update(Long sessionExamId, SessionExamUpdateRequest request) {
    User currentUser = authService.getCurrentUser();
    LocalDateTime currentDate = LocalDateTime.now();
    SessionExam entity = sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(
            sessionExamId, currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
    if (currentDate.isAfter(entity.getEndDate())) {
      throw new AppException(
          messageUtils.getMessage(AppConst.MessageConst.CURRENT_DATE_AFTER_END_DATE),
          messageUtils.getMessage(AppConst.MessageConst.CURRENT_DATE_AFTER_END_DATE),
          HttpStatus.BAD_REQUEST);
    }
    if (!classroomRepository.existsByClassroomIdAndTeacherId(entity.getClassId(),
        currentUser.getId())) {
      throw new AppException(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN),
          messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.BAD_REQUEST);
    }
    // Get current or new values
    LocalDateTime start =
        request.getStartDate() != null ? request.getStartDate() : entity.getStartDate();
    LocalDateTime end = request.getEndDate() != null ? request.getEndDate() : entity.getEndDate();
    Long duration = request.getDuration() != null ? request.getDuration() : entity.getDuration();

    // Validate if any time-related field changes
    if (request.getStartDate() != null || request.getEndDate() != null
        || request.getDuration() != null) {
      validateTimeForUpdate(start, end, duration);
    }

    // Update fields
    if (request.getStartDate() != null) {
      entity.setStartDate(start);
    }
    if (request.getEndDate() != null) {
      entity.setEndDate(end);
    }
    if (request.getTitle() != null) {
      entity.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      entity.setDescription(request.getDescription());
    }
    if (request.getDuration() != null) {
      entity.setDuration(request.getDuration());
    }
    if (request.getExamMode() != null) {
      entity.setExamMode(request.getExamMode());
    }
    if (request.getQuestionOrderMode() != null) {
      entity.setQuestionOrderMode(request.getQuestionOrderMode());
    }
    if (request.getIsInstantlyResult() != null) {
      entity.setIsInstantlyResult(request.getIsInstantlyResult());
    }

    SessionExam updated = sessionExamRepository.saveAndFlush(entity);
    return toResponse(updated);
  }

  @Override
  public SessionExamDetailResponse getDetail(Long sessionExamId) {
    User currentUser = authService.getCurrentUser();
    SessionExam entity = sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(
            sessionExamId, currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    SessionExamDetailResponse r = new SessionExamDetailResponse();
    r.setSessionExamId(entity.getSessionExamId());
    r.setClassId(entity.getClassId());
    r.setStartDate(entity.getStartDate());
    r.setEndDate(entity.getEndDate());
    r.setTitle(entity.getTitle());
    r.setDescription(entity.getDescription());
    r.setDuration(entity.getDuration());
    r.setExamMode(entity.getExamMode());
    r.setStatus(entity.getStatus());
    r.setQuestionOrderMode(entity.getQuestionOrderMode());
    r.setIsInstantlyResult(entity.getIsInstantlyResult());
    r.setCreatedBy(entity.getCreatedBy());
    r.setExamId(entity.getExamId());

    if (entity.getExamId() != null) {
      Exam exam = examRepository.findByExamIdAndIsDeletedIsFalse(entity.getExamId())
          .orElse(null);
      if (exam != null) {
        r.setExamTitle(exam.getTitle());
        r.setExamDescription(exam.getDescription());
        r.setExamCreatedBy(exam.getCreatedBy());
        if (exam.getSubject() != null) {
          r.setSubjectId(exam.getSubjectId());
          r.setSubjectName(exam.getSubject().getSubjectName());
          r.setSubjectCode(exam.getSubject().getSubjectCode());
        }
        if (exam.getCreatedBy() != null) {
          userRepository.findById(exam.getCreatedBy()).ifPresent(u -> {
            r.setExamCreatorFullName(u.getFullName());
            r.setExamCreatorEmail(u.getEmail());
          });
        }
      }
    }
    return r;
  }

  @Override
  public void delete(Long sessionExamId) {
    User currentUser = authService.getCurrentUser();
    SessionExam entity = sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(
            sessionExamId, currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
    entity.setIsDeleted(true);
    sessionExamRepository.saveAndFlush(entity);
  }

  @Override
  public ResponseListData<SessionExamSearchTeacherResponse> searchSessionExamByTeacher(
      BaseFilterSearchRequest<SessionExamSearchTeacherRequest> request) {
    log.info("Start service to search session exam by teacher");
    User user = authService.getCurrentUser();
    SessionExamSearchTeacherRequestDTO dto = request.getFilters().toDTO();
    dto.setCreatedBy(user.getId());

    Pageable pageable = request.getPagination().getPagingMeta().toPageable();
    Page<SessionExamTeacherQueryDTO> queryDTOS = sessionExamRepository.searchByTeacher(dto,
        pageable);
    List<SessionExamSearchTeacherResponse> response = queryDTOS.stream()
        .map(SessionExamSearchTeacherResponse::fromDTO)
        .toList();
    PagingMeta pagingMeta = request.getPagination().getPagingMeta();
    pagingMeta.setTotalRows(queryDTOS.getTotalElements());
    pagingMeta.setTotalPages(queryDTOS.getTotalPages());

    log.info("End service search session exam by teacher");
    return new ResponseListData<>(response, pagingMeta);
  }

  @Override
  public ResponseListData<SessionExamSearchStudentResponse> searchSessionExamByStudent(
      BaseFilterSearchRequest<SessionExamSearchStudentRequest> request) {
    log.info("Start service to search session exam by student");

    User user = authService.getCurrentUser();
    SessionExamSearchStudentRequestDTO dto = request.getFilters().toDTO();
    dto.setStudentId(user.getId());
    if (dto.getClassId() != null
        && !classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
        dto.getClassId(), user.getId(), ClassMemberStatus.ACTIVE
    )) {
      throw new AppException(AppConst.MessageConst.NOT_FOUND,
          messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }
    Pageable pageable = request.getPagination().getPagingMeta().toPageable();
    Page<SessionExamStudentQueryDTO> queryDTOS = sessionExamRepository.searchByStudent(dto,
        pageable);
    List<SessionExamSearchStudentResponse> response = queryDTOS.stream()
        .map(SessionExamSearchStudentResponse::fromDTO)
        .toList();
    PagingMeta pagingMeta = request.getPagination().getPagingMeta();
    pagingMeta.setTotalRows(queryDTOS.getTotalElements());
    pagingMeta.setTotalPages(queryDTOS.getTotalPages());

    log.info("End service search session exam by student");
    return new ResponseListData<>(response, pagingMeta);
  }

  private void validateTimeForCreate(LocalDateTime start, LocalDateTime end, Long duration) {
    LocalDateTime now = LocalDateTime.now();

    // Validate start and end not null
    if (start == null || end == null) {
      throw new AppException(AppConst.MessageConst.REQUIRED_FIELD_EMPTY,
          messageUtils.getMessage(AppConst.MessageConst.REQUIRED_FIELD_EMPTY),
          HttpStatus.BAD_REQUEST);
    }

    // Validate startDate > currentTime + 5 minutes
    LocalDateTime minStartTime = now.plusMinutes(AppConst.COUNTDOWN_M);
    if (start.isBefore(minStartTime)) {
      throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
          "Start date must be at least 5 minutes from now", HttpStatus.BAD_REQUEST);
    }

    // Validate end > start
    if (!end.isAfter(start)) {
      throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
          "End date must be after start date", HttpStatus.BAD_REQUEST);
    }

    // Validate (endDate - startDate) >= duration (in minutes)
    if (duration != null && duration > 0) {
      long diffMinutes = java.time.Duration.between(start, end).toMinutes();
      if (diffMinutes < duration) {
        throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
            "Time range (end - start) must be at least " + duration + " minutes",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  private void validateTimeForUpdate(LocalDateTime start, LocalDateTime end, Long duration) {
    // Validate start and end not null
    if (start == null || end == null) {
      throw new AppException(AppConst.MessageConst.REQUIRED_FIELD_EMPTY,
          messageUtils.getMessage(AppConst.MessageConst.REQUIRED_FIELD_EMPTY),
          HttpStatus.BAD_REQUEST);
    }

    // Validate end > start
    if (!end.isAfter(start)) {
      throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
          "End date must be after start date", HttpStatus.BAD_REQUEST);
    }

    // Validate (endDate - startDate) >= duration (in minutes)
    if (duration != null && duration > 0) {
      long diffMinutes = Duration.between(start, end).toMinutes();
      if (diffMinutes < duration) {
        throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
            "Time range (end - start) must be at least " + duration + " minutes",
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  private SessionExamResponse toResponse(SessionExam e) {
    SessionExamResponse r = new SessionExamResponse();
    r.setSessionExamId(e.getSessionExamId());
    r.setClassId(e.getClassId());
    r.setStartDate(e.getStartDate());
    r.setEndDate(e.getEndDate());
    r.setTitle(e.getTitle());
    r.setDescription(e.getDescription());
    r.setDuration(e.getDuration());
    r.setExamMode(e.getExamMode());
    r.setQuestionOrderMode(e.getQuestionOrderMode());
    r.setIsInstantlyResult(e.getIsInstantlyResult());
    r.setCreatedBy(e.getCreatedBy());
    r.setExamId(e.getExamId());
    return r;
  }

  @Override
  @Transactional
  public ExamQuestionsResponse getExamQuestions(Long sessionExamId) {
    log.info("Getting exam questions for session exam {}", sessionExamId);
    User currentUser = authService.getCurrentUser();

    // Get session exam
    SessionExam sessionExam = sessionExamRepository.findById(sessionExamId)
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Check if student is member of class.
    classMemberRepository
        .findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
            sessionExam.getClassId(),
            currentUser.getId(),
            ClassMemberRole.STUDENT,
            ClassMemberStatus.ACTIVE
        )
        .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
            messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

    // Get or create StudentSessionExam
    Optional<StudentSessionExam> studentSessionExamOpt = studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, currentUser.getId());

    StudentSessionExam studentSessionExam;
    if (studentSessionExamOpt.isEmpty()) {
      // Student not added to exam - throw error
      throw new AppException(AppConst.MessageConst.NOT_FOUND,
          "Bạn chưa được thêm vào kỳ thi này", HttpStatus.BAD_REQUEST);
    }
    studentSessionExam = studentSessionExamOpt.get();

    if (studentSessionExam.getSubmissionStatus() == ExamSubmissionStatus.SUBMITTED) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Bạn đã nộp bài thi này rồi", HttpStatus.FORBIDDEN);
    }

    // Record exam start time on first load
    boolean isFirstTime = false;
    if (studentSessionExam.getExamStartTime() == null) {
      LocalDateTime examStartTime = LocalDateTime.now();
      studentSessionExam.setExamStartTime(examStartTime);
      studentSessionExam.setSubmissionStatus(ExamSubmissionStatus.NOT_SUBMITTED);
      studentSessionExamRepository.saveAndFlush(studentSessionExam);
      isFirstTime = true;
    }

    String examContentKey = RedisUtils.studentExamContent(sessionExamId, currentUser.getId());
    String statusKey = RedisUtils.studentStatus(sessionExamId, currentUser.getId());

    ExamQuestionsResponse examResponse;

    // Check if exam content exists in Redis (student has loaded before)
    ExamQuestionsResponse cachedExam = redisService.get(examContentKey,
        ExamQuestionsResponse.class);

    if (cachedExam != null && !isFirstTime) {
      // Load from Redis if exists (student returning) - selectedAnswerIds already included
      log.info("Loading exam content from Redis for student {} in session exam {}",
          currentUser.getId(), sessionExamId);
      examResponse = cachedExam;
    } else {
      // First time loading - get questions from DB and process order
      log.info("First time loading exam for student {} in session exam {}",
          currentUser.getId(), sessionExamId);

      List<ExamQuestionSnapshotResponse> questions = examQuestionSnapshotService.getAllQuestions(
          sessionExamId, sessionExam.getQuestionOrderMode());

      // Process question order based on QuestionOrderMode
      if (sessionExam.getQuestionOrderMode() == QuestionOrderMode.RANDOM) {
        questions = new ArrayList<>(questions);
        Collections.shuffle(questions, new Random());
        log.info("Shuffled {} questions for RANDOM mode", questions.size());
      }

      // Initialize selectedAnswerIds as null for all questions
      for (ExamQuestionSnapshotResponse question : questions) {
        question.setSelectedAnswerIds(null);
      }

      // Build exam response
      examResponse = ExamQuestionsResponse.builder()
          .sessionExamId(sessionExamId)
          .examId(sessionExam.getExamId())
          .title(sessionExam.getTitle())
          .duration(sessionExam.getDuration())
          .examStartTime(studentSessionExam.getExamStartTime())
          .endDate(sessionExam.getEndDate())
          .canSubmit(studentSessionExam.getSubmissionStatus() != ExamSubmissionStatus.SUBMITTED)
          .submissionStatus(studentSessionExam.getSubmissionStatus())
          .questions(questions)
          .build();

      // Save entire exam content to Redis with TTL
      LocalDateTime examStartTime = studentSessionExam.getExamStartTime();
      long ttlSeconds = Duration.between(examStartTime,
          examStartTime.plusMinutes(sessionExam.getDuration())).getSeconds();

      if (ttlSeconds > 0) {
        try {
          // Save exam content (with selectedAnswerIds = null initially)
          redisService.set(examContentKey, examResponse, ttlSeconds,
              TimeUnit.SECONDS);

          // Initialize student status
          Map<String, Object> statusMap = new HashMap<>();
          statusMap.put("status", ExamSubmissionStatus.NOT_SUBMITTED.name());
          statusMap.put("joinedAt", examStartTime.toString());
          statusMap.put("examStartTime", examStartTime.toString());
          statusMap.put("lastSaveAt", System.currentTimeMillis());
          redisService.hSetAll(statusKey, statusMap);
          redisService.expire(statusKey, ttlSeconds, TimeUnit.SECONDS);

          log.info(
              "Saved exam content to Redis for student {} in session exam {} with TTL {} seconds",
              currentUser.getId(), sessionExamId, ttlSeconds);
        } catch (Exception e) {
          log.error("Error saving exam content to Redis: {}", e.getMessage());
        }
      }
    }

    return examResponse;
  }

  @Override
  @Transactional
  public ExamSaveAnswersResponse saveExamAnswers(ExamSaveAnswersRequest request) {
    log.info("Saving exam answers to Redis for session exam {}", request.getSessionExamId());
    User currentUser = authService.getCurrentUser();

    // Get session exam
    SessionExam sessionExam = sessionExamRepository.findById(request.getSessionExamId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Check if it's FLEX exam
    if (sessionExam.getExamMode() != ExamMode.FLEXIBLE) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Chỉ có thể lưu kết quả cho bài thi FLEXIBLE", HttpStatus.FORBIDDEN);
    }

    // Get StudentSessionExam
    StudentSessionExam studentSessionExam = studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(request.getSessionExamId(),
            currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Check if already submitted
    if (studentSessionExam.getSubmissionStatus() == ExamSubmissionStatus.SUBMITTED) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Bạn đã nộp bài thi này rồi", HttpStatus.FORBIDDEN);
    }

    // Check if exam has started
    if (studentSessionExam.getExamStartTime() == null) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Bạn chưa bắt đầu làm bài thi", HttpStatus.FORBIDDEN);
    }

    // Check if still within exam time
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime examEndTime = studentSessionExam.getExamStartTime()
        .plusMinutes(sessionExam.getDuration());
    if (now.isAfter(examEndTime)) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Đã hết thời gian làm bài", HttpStatus.FORBIDDEN);
    }

    // Update exam content in Redis with selected answers
    String examContentKey = RedisUtils.studentExamContent(request.getSessionExamId(),
        currentUser.getId());
    ExamQuestionsResponse examContent = redisService.get(examContentKey,
        ExamQuestionsResponse.class);

    if (examContent == null) {
      throw new AppException(AppConst.MessageConst.NOT_FOUND,
          "Không tìm thấy đề thi trong Redis. Vui lòng tải lại đề thi.", HttpStatus.BAD_REQUEST);
    }

    // Update selectedAnswerIds for each question
    Map<Long, List<Long>> answerMap = new HashMap<>();
    for (ExamSaveAnswersRequest.AnswerData answer : request.getAnswers()) {
      answerMap.put(answer.getQuestionSnapshotId(), answer.getSelectedAnswerIds());
    }

    // Update selectedAnswerIds in exam content
    for (ExamQuestionSnapshotResponse question : examContent.getQuestions()) {
      if (answerMap.containsKey(question.getId())) {
        question.setSelectedAnswerIds(answerMap.get(question.getId()));
      }
    }

    // Save updated exam content back to Redis
    long remainingSeconds = Duration.between(now, examEndTime).getSeconds();
    if (remainingSeconds > 0) {
      // Update last save time in student status
      String statusKey = RedisUtils.studentStatus(request.getSessionExamId(), currentUser.getId());
      long currentTimeMillis = System.currentTimeMillis();

      // Check if remaining time to exam end is less than 1 minute
      long oneMinuteInSeconds = 60; // 1 phút = 60 giây
      long threeMinutesInSeconds = 3 * 60; // 3 phút = 180 giây

      if (remainingSeconds < oneMinuteInSeconds) {
        // Nếu thời gian còn lại < 1 phút, reset TTL = remainingSeconds + 3 phút
        // (Để đảm bảo dữ liệu không bị xóa quá sớm, cho phép kéo dài thêm 3 phút)
        long originalRemainingSeconds = remainingSeconds;
        remainingSeconds = remainingSeconds + threeMinutesInSeconds;
        log.info(
            "Remaining time to exam end is {} seconds (< 1 minute), extending TTL by 3 minutes to {} seconds for student {} in session exam {}",
            originalRemainingSeconds, remainingSeconds, currentUser.getId(),
            request.getSessionExamId());
      }

      // Save exam content with updated TTL
      redisService.set(examContentKey, examContent, remainingSeconds,
          java.util.concurrent.TimeUnit.SECONDS);
      log.info(
          "Updated exam content with {} answers in Redis for student {} in session exam {} with TTL {} seconds",
          answerMap.size(), currentUser.getId(), request.getSessionExamId(), remainingSeconds);

      // Update last save time in student status
      redisService.hSet(statusKey, "lastSaveAt", currentTimeMillis);

      // Ensure status key TTL is still valid
      Long currentTtl = redisService.getExpire(statusKey, java.util.concurrent.TimeUnit.SECONDS);
      if (currentTtl == null || currentTtl < remainingSeconds) {
        redisService.expire(statusKey, remainingSeconds, java.util.concurrent.TimeUnit.SECONDS);
      }
    }

    log.info("Successfully saved exam answers to Redis for session exam {}",
        request.getSessionExamId());
    return ExamSaveAnswersResponse.builder()
        .sessionExamId(request.getSessionExamId())
        .studentId(currentUser.getId())
        .savedAt(LocalDateTime.now())
        .message("Lưu kết quả thành công")
        .build();
  }

  @Override
  @Transactional
  public ExamSubmissionResponse submitExam(ExamSubmissionRequest request) {
    log.info("Submitting exam for session exam {}", request.getSessionExamId());
    User currentUser = authService.getCurrentUser();

    // Get session exam
    SessionExam sessionExam = sessionExamRepository.findById(request.getSessionExamId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Get StudentSessionExam
    StudentSessionExam studentSessionExam = studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(request.getSessionExamId(),
            currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Check if already submitted - only allow submission once
    if (studentSessionExam.getSubmissionStatus() == ExamSubmissionStatus.SUBMITTED) {
      throw new AppException(AppConst.MessageConst.FORBIDDEN,
          "Bạn đã nộp bài thi này rồi", HttpStatus.FORBIDDEN);
    }

    // Get all exam question snapshots for this session exam
    List<ExamQuestionSnapshot> questionSnapshots = examQuestionSnapshotRepository
        .findAllBySessionExamId(request.getSessionExamId());

    // Calculate score: (Số câu đúng / Tổng số câu) * 10
    int totalQuestions = questionSnapshots.size();
    int correctCount = 0;
    Map<Long, List<Long>> submittedAnswers = new HashMap<>();
    if (request.getAnswers() != null) {
      for (ExamSubmissionRequest.AnswerSubmission answerSubmission : request.getAnswers()) {
        List<Long> selectedIds = answerSubmission.getSelectedAnswerIds() != null
            ? new ArrayList<>(answerSubmission.getSelectedAnswerIds())
            : new ArrayList<>();
        submittedAnswers.put(answerSubmission.getQuestionSnapshotId(), selectedIds);
      }
    }

    for (ExamQuestionSnapshot questionSnapshot : questionSnapshots) {
      List<Long> selectedIds = submittedAnswers.get(questionSnapshot.getId());
      if (selectedIds == null) {
        selectedIds = Collections.emptyList();
      }

      List<ExamQuestionAnswerSnapshot> correctAnswers = questionSnapshot.getExamQuestionAnswers()
          .stream()
          .filter(ExamQuestionAnswerSnapshot::getIsCorrect)
          .toList();

      boolean isCorrect = false;
      if (!selectedIds.isEmpty()) {
        List<Long> correctIds = correctAnswers.stream()
            .map(ExamQuestionAnswerSnapshot::getId)
            .toList();

        if (questionSnapshot.getQuestionType() == QuestionType.MULTI_CHOICE) {
          isCorrect = selectedIds.size() == 1 && correctIds.containsAll(selectedIds)
              && correctIds.size() == 1;
        } else {
          isCorrect =
              selectedIds.size() == correctIds.size() && correctIds.containsAll(selectedIds);
        }
      }

      if (isCorrect) {
        correctCount++;
      }

    }

    // Tính điểm: (Số câu đúng / Tổng số câu) * 10
    double totalScore = totalQuestions > 0 ? (double) correctCount / totalQuestions * 10.0 : 0.0;

    // Merge previously saved answers (if any) to ensure we persist full selection
    String examContentKey = RedisUtils.studentExamContent(request.getSessionExamId(),
        currentUser.getId());
    ExamQuestionsResponse cachedExamContent = redisService.get(examContentKey,
        ExamQuestionsResponse.class);
    Map<Long, List<Long>> persistedSelectedMap = mergeSelectedAnswers(studentSessionExam,
        cachedExamContent, request.getAnswers());

    SessionExamSubmissionDTO sessionExamSubmissionDTO = buildSubmissionResultPayload(questionSnapshots, persistedSelectedMap);
    String submissionResultJson = JsonUtils.convertToJson(sessionExamSubmissionDTO);

    // Update StudentSessionExam
    studentSessionExam.setSubmissionStatus(ExamSubmissionStatus.SUBMITTED);
    studentSessionExam.setScore(totalScore);
    studentSessionExam.setSubmissionTime(LocalDateTime.now());
    studentSessionExam.setSubmissionResult(submissionResultJson);
    studentSessionExamRepository.saveAndFlush(studentSessionExam);

    // Delete all Redis data for this student's exam session after submission
    try {
      String statusKey = RedisUtils.studentStatus(request.getSessionExamId(), currentUser.getId());
      String questionOrderKey = RedisUtils.studentQuestionOrder(request.getSessionExamId(),
          currentUser.getId());

      // Delete all related Redis keys
      redisService.delete(examContentKey);
      redisService.delete(statusKey);
      redisService.delete(questionOrderKey);

      log.info(
          "Deleted all Redis keys (exam content, status, order) for student {} after submitting exam session {}",
          currentUser.getId(), request.getSessionExamId());
    } catch (Exception e) {
      log.warn("Error deleting Redis keys after submission for student {} in session exam {}: {}",
          currentUser.getId(), request.getSessionExamId(), e.getMessage());
      // Don't fail submission if Redis deletion fails
    }

    return ExamSubmissionResponse.builder()
        .studentSessionExamId(studentSessionExam.getStudentSessionExamId())
        .sessionExamId(request.getSessionExamId())
        .score(totalScore)
        .submissionTime(studentSessionExam.getSubmissionTime())
        .message("Nộp bài thành công!")
        .build();
  }

  private SessionExamSubmissionDTO buildSubmissionResultPayload(
      List<ExamQuestionSnapshot> questionSnapshots,
      Map<Long, List<Long>> selectedAnswersMap) {
    List<ExamQuestionDTO> questions = questionSnapshots.stream()
            .map(ExamQuestionDTO::fromEntity)
            .toList();
    return SessionExamSubmissionDTO.builder()
            .questions(questions)
            .studentAnswers(selectedAnswersMap)
            .build();
  }

  private Map<Long, List<Long>> mergeSelectedAnswers(StudentSessionExam studentSessionExam,
      ExamQuestionsResponse cachedContent,
      List<ExamSubmissionRequest.AnswerSubmission> submissions) {
    Map<Long, List<Long>> selectedMap = new HashMap<>();
    selectedMap.putAll(extractSelectedAnswerMap(cachedContent));
    selectedMap.putAll(extractSelectedAnswerMap(readSubmissionResult(studentSessionExam)));
    if (submissions != null) {
      for (ExamSubmissionRequest.AnswerSubmission submission : submissions) {
        List<Long> selected = submission.getSelectedAnswerIds() == null
            ? new ArrayList<>()
            : new ArrayList<>(submission.getSelectedAnswerIds());
        selectedMap.put(submission.getQuestionSnapshotId(), selected);
      }
    }
    return selectedMap;
  }

  private Map<Long, List<Long>> extractSelectedAnswerMap(ExamQuestionsResponse examContent) {
    Map<Long, List<Long>> map = new HashMap<>();
    if (examContent == null || examContent.getQuestions() == null) {
      return map;
    }
    for (ExamQuestionSnapshotResponse question : examContent.getQuestions()) {
      if (question.getSelectedAnswerIds() != null) {
        map.put(question.getId(), new ArrayList<>(question.getSelectedAnswerIds()));
      }
    }
    return map;
  }

  private ExamQuestionsResponse readSubmissionResult(StudentSessionExam studentSessionExam) {
    if (studentSessionExam.getSubmissionResult() == null) {
      return null;
    }
    try {
      return objectMapper.readValue(studentSessionExam.getSubmissionResult(),
          ExamQuestionsResponse.class);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to parse submission result for student {} in session exam {}",
          studentSessionExam.getStudentId(), studentSessionExam.getSessionExamId(),
          ex.getMessage());
      return null;
    }
  }

  private ObjectMapper createSubmissionObjectMapper() {
    ObjectMapper mapper = objectMapper.copy();
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
      @Override
      public boolean hasIgnoreMarker(AnnotatedMember m) {
        return false;
      }
    });
    return mapper;
  }

  @Override
  public ResponseListData<ExamResultResponse> getExamResults(Long sessionExamId,
      BaseFilterSearchRequest<?> request) {
    log.info("Getting exam results for session exam {}", sessionExamId);
    User currentUser = authService.getCurrentUser();

    // Check if user is teacher who created the session exam
    SessionExam sessionExam = sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(
            sessionExamId, currentUser.getId())
        .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
            messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

    // Get pagination
    Pageable pageable =
        request.getPagination() != null && request.getPagination().getPagingMeta() != null
            ? request.getPagination().getPagingMeta().toPageable()
            : Pageable.unpaged();

    Page<Object[]> page = studentSessionExamRepository.getExamResults(
        sessionExamId,
        sessionExam.getClassId(),
        pageable
    );

    // Map results from Object[] to ExamResultResponse
    List<ExamResultResponse> results = page.getContent().stream()
        .map(row -> {
          Long studentSessionExamId = row[0] != null ? ((Number) row[0]).longValue() : null;
          Long resultSessionExamId = row[1] != null ? ((Number) row[1]).longValue() : sessionExamId;
          Long studentId = row[2] != null ? ((Number) row[2]).longValue() : null;
          String fullName = (String) row[3];
          String username = (String) row[4];
          String code = (String) row[5];
          String email = (String) row[6];
          String avatarUrl = (String) row[7];
          Double score = row[8] != null ? ((Number) row[8]).doubleValue() : null;
          ExamSubmissionStatus status = row[9] != null
              ? (ExamSubmissionStatus) row[9]
              : ExamSubmissionStatus.NOT_SUBMITTED;

          LocalDateTime examStartTime = null;
          if (row[10] != null) {
            if (row[10] instanceof Timestamp) {
              examStartTime = ((Timestamp) row[10]).toLocalDateTime();
            } else if (row[10] instanceof LocalDateTime) {
              examStartTime = (LocalDateTime) row[10];
            }
          }

          LocalDateTime submissionTime = null;
          if (row[11] != null) {
            if (row[11] instanceof Timestamp) {
              submissionTime = ((Timestamp) row[11]).toLocalDateTime();
            } else if (row[11] instanceof LocalDateTime) {
              submissionTime = (LocalDateTime) row[11];
            }
          }

          LocalDateTime createdAt = null;
          if (row[12] != null) {
            if (row[12] instanceof Timestamp) {
              createdAt = ((Timestamp) row[12]).toLocalDateTime();
            } else if (row[12] instanceof LocalDateTime) {
              createdAt = (LocalDateTime) row[12];
            }
          }

          return ExamResultResponse.builder()
              .studentSessionExamId(studentSessionExamId)
              .sessionExamId(resultSessionExamId)
              .studentId(studentId)
              .studentFullName(fullName)
              .studentUsername(username)
              .studentCode(code)
              .studentEmail(email)
              .studentAvatarUrl(avatarUrl)
              .score(score)
              .submissionStatus(status)
              .examStartTime(examStartTime)
              .submissionTime(submissionTime)
              .createdAt(createdAt)
              .build();
        })
        .toList();

    PagingMeta pagingMeta = new PagingMeta(
        page.getNumber() + 1,
        page.getSize()
    );
    pagingMeta.setTotalRows(page.getTotalElements());
    pagingMeta.setTotalPages(page.getTotalPages());

    return new ResponseListData<>(results, pagingMeta);
  }

  @Override
  public StudentExamOverviewResponse getStudentExamOverview(Long sessionExamId) {
    log.info("Getting student exam overview for session exam {}", sessionExamId);
    User currentUser = authService.getCurrentUser();
    LocalDateTime now = LocalDateTime.now();

    // Get session exam
    SessionExam sessionExam = sessionExamRepository.findById(sessionExamId)
        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

    // Check if student is member of the classroom
    classMemberRepository
        .findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
            sessionExam.getClassId(),
            currentUser.getId(),
            ClassMemberRole.STUDENT,
            ClassMemberStatus.ACTIVE
        )
        .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
            messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.BAD_REQUEST));

    // Get StudentSessionExam (may be null if student not added to exam)
    Optional<StudentSessionExam> studentSessionExamOpt = studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, currentUser.getId());

    // Determine submission status
    ExamSubmissionStatus submissionStatus = ExamSubmissionStatus.NOT_STARTED;
    LocalDateTime examStartTime = null;
    LocalDateTime submissionTime = null;
    Double score = null;
    Long studentSessionExamId = null;

    if (studentSessionExamOpt.isPresent()) {
      StudentSessionExam studentSessionExam = studentSessionExamOpt.get();
      submissionStatus = studentSessionExam.getSubmissionStatus();
      examStartTime = studentSessionExam.getExamStartTime();
      submissionTime = studentSessionExam.getSubmissionTime();
      score = studentSessionExam.getScore();
      studentSessionExamId = studentSessionExam.getStudentSessionExamId();
    }

    // Calculate time-related flags
    boolean isWithinExamPeriod;
    if (ExamMode.FLEXIBLE.equals(sessionExam.getExamMode())) {
        isWithinExamPeriod = !now.isBefore(sessionExam.getStartDate()) && !now.isAfter(sessionExam.getEndDate());
    } else {
        isWithinExamPeriod = !now.isBefore(sessionExam.getStartDate().minusMinutes(AppConst.COUNTDOWN_M)) && !now.isAfter(sessionExam.getEndDate());
    }

    boolean isTimeExpired = false;
    Long remainingTimeInMinutes = null;

    if (examStartTime != null && submissionStatus == ExamSubmissionStatus.NOT_SUBMITTED) {
      LocalDateTime examEndTime = examStartTime.plusMinutes(sessionExam.getDuration());
      isTimeExpired = now.isAfter(examEndTime);

      if (!isTimeExpired) {
        Duration remaining = Duration.between(now, examEndTime);
        remainingTimeInMinutes = remaining.toMinutes();
        if (remainingTimeInMinutes < 0) {
          remainingTimeInMinutes = 0L;
        }
      }
    }

    // Calculate UI flags
    // Student must be added to exam (studentSessionExamOpt.isPresent()) to start exam
    boolean canStartExam = studentSessionExamOpt.isPresent()
        && submissionStatus == ExamSubmissionStatus.NOT_STARTED
        && isWithinExamPeriod;
    boolean canContinueExam = submissionStatus == ExamSubmissionStatus.NOT_SUBMITTED
        && examStartTime != null
        && !isTimeExpired;

    // If not instantly result, hide score even if submitted
    Double displayScore = null;
    if (submissionStatus == ExamSubmissionStatus.SUBMITTED
        && Boolean.TRUE.equals(sessionExam.getIsInstantlyResult())) {
      displayScore = score;
    }

    // Get total number of questions for this session exam
    Long questionCount = examQuestionSnapshotRepository.countBySessionExamId(sessionExamId);
    Integer totalQuestions = questionCount != null ? questionCount.intValue() : 0;

    // Build response
    return StudentExamOverviewResponse.builder()
        .sessionExamId(sessionExam.getSessionExamId())
        .examId(sessionExam.getExamId())
        .title(sessionExam.getTitle())
        .description(sessionExam.getDescription())
        .duration(sessionExam.getDuration())
        .startDate(sessionExam.getStartDate())
        .endDate(sessionExam.getEndDate())
        .examMode(sessionExam.getExamMode())
        .questionOrderMode(sessionExam.getQuestionOrderMode())
        .isInstantlyResult(sessionExam.getIsInstantlyResult())
        .totalQuestions(totalQuestions)
        .studentSessionExamId(studentSessionExamId)
        .submissionStatus(submissionStatus)
        .examStartTime(examStartTime)
        .submissionTime(submissionTime)
        .score(displayScore)
        .canStartExam(canStartExam)
        .canContinueExam(canContinueExam)
        .build();
  }

  @Override
  @Transactional
  public void startLiveSessionExam(SessionExam sessionExam) {

    String stateKey = RedisUtils.examSessionState(sessionExam.getSessionExamId());
    if (redisService.exists(stateKey) || LocalDateTime.now().isAfter(sessionExam.getEndDate())) {
      throw new AppException(
          MessageConst.EXAM_ALREADY_STARTED,
          messageUtils.getMessage(MessageConst.EXAM_ALREADY_STARTED),
          HttpStatus.BAD_REQUEST);
    }
    List<StudentSessionExam> studentSessionExams = studentSessionExamRepository.findAllBySessionExamId(
        sessionExam.getSessionExamId());

    // Build state DTO
    SessionExamStateDTO sessionExamStateDTO = SessionExamStateDTO.builder()
        .instructorId(sessionExam.getCreatedBy())
        .sessionExamId(sessionExam.getSessionExamId())
        .title(sessionExam.getTitle())
        .description(sessionExam.getDescription())
        .questionOrderMode(sessionExam.getQuestionOrderMode())
        .isInstantlyResult(sessionExam.getIsInstantlyResult())
        .countdownStartAt(LocalDateTime.now())
        .duration(sessionExam.getDuration())
        .totalStudents(studentSessionExams.size())
        .build();
    long ttl = this.toSeconds(sessionExam.getDuration()) + AppConst.COUNTDOWN_S + AppConst.TTL_BUFFER;
    // Lưu thông tin ca thi
    this.saveExamStateToRedis(stateKey, sessionExamStateDTO, ttl);
    // Lưu thông tin đề thi và đáp án vào redis
    SessionExamContentDTO examContent = this.saveSessionExamContentToRedis(sessionExam, ttl);
    // Lưu validation map
    this.saveValidationMapToRedis(sessionExam.getSessionExamId(), examContent, ttl);
    // Lưu danh sách sinh viên ĐỦ ĐIỀU KIỆN
    String eligibleStudentsKey = RedisUtils.examSessionEligibleStudents(
        sessionExam.getSessionExamId());
    redisService.delete(eligibleStudentsKey);
    for (StudentSessionExam studentSessionExam : studentSessionExams) {
      redisService.sAdd(eligibleStudentsKey, String.valueOf(studentSessionExam.getStudentId()));
    }
    redisService.expire(eligibleStudentsKey, ttl, TimeUnit.SECONDS);

    sessionExam.setStartDate(sessionExamStateDTO.getReadyAt());
    sessionExam.setEndDate(sessionExamStateDTO.getExamEndAt());
    sessionExam.setStatus(SessionExamStatus.ONGOING);
    sessionExam.setStartScheduler(true);
    sessionExam.setStartSchedulerTime(LocalDateTime.now());
    sessionExamRepository.save(sessionExam);
  }

  @Override
  @Transactional
  public JoinExamResponse joinSessionExam(Long sessionExamId) {
    // Get current user
    User currentUser = authService.getCurrentUser();

    // Get session exam
    SessionExam sessionExam = sessionExamRepository
        .findBySessionExamIdAndIsDeletedFalse(sessionExamId)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

      // Kiểm tra có đủ điều kiện không
      String eligibleStudentsKey = RedisUtils.examSessionEligibleStudents(sessionExam.getSessionExamId());
      String studentIdStr = currentUser.getId().toString();
      Boolean isEligible = redisService.sIsMember(eligibleStudentsKey, studentIdStr);

      if (Boolean.FALSE.equals(isEligible)) {
          throw new AppException(
                  MessageConst.NOT_IN_CLASSROOM,
                  messageUtils.getMessage(MessageConst.NOT_IN_CLASSROOM),
                  HttpStatus.BAD_REQUEST);
      }

    // Get state from Redis
    SessionExamStateDTO sessionExamState = this.getExamState(sessionExam.getSessionExamId());

    if (sessionExamState == null) {
      return JoinExamResponse.builder()
          .sessionExamId(sessionExamId)
          .title(sessionExam.getTitle())
          .description(sessionExam.getDescription())
          .duration(sessionExam.getDuration())
          .phase(SessionExamPhase.NOT_STARTED)
          .build();
    }

    // Compute phase
    SessionExamPhase phase = sessionExamState.computePhase();

    if (SessionExamPhase.NOT_STARTED.equals(phase)) {
      return JoinExamResponse.builder()
          .sessionExamId(sessionExamId)
          .title(sessionExam.getTitle())
          .description(sessionExam.getDescription())
          .duration(sessionExam.getDuration())
          .phase(SessionExamPhase.NOT_STARTED)
          .build();
    }

    if (SessionExamPhase.ENDED.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_ENDED,
          messageUtils.getMessage(MessageConst.EXAM_ENDED),
          HttpStatus.BAD_REQUEST);
    }
    long ttl = Duration.between(LocalDateTime.now(), sessionExamState.getExamEndAt())
              .getSeconds() + AppConst.TTL_BUFFER;
    // Kiểm tra đã join chưa
    String joinedStudentsKey = RedisUtils.examSessionJoinedStudents(sessionExamId);
    Boolean alreadyJoined = redisService.sIsMember(joinedStudentsKey, studentIdStr);

    String sessionToken;
    String statusKey = RedisUtils.studentStatus(sessionExamId, currentUser.getId());

    if (Boolean.TRUE.equals(alreadyJoined)) {
      // ĐÃ JOIN, Lấy token từ status
      log.info("Student rejoining: sessionExamId={}, student={}",
          sessionExamId, currentUser.getEmail());

      sessionToken = redisService.parseString(
          redisService.hGet(statusKey, FieldConst.SESSION_TOKEN));

      if (sessionToken == null) {
        // Token bị mất
        sessionToken = generateSessionToken();

        StudentInfoDTO tokenInfo = StudentInfoDTO.fromUser(currentUser);
        String tokenKey = RedisUtils.examSessionToken(sessionExamId, sessionToken);
        redisService.set(tokenKey, tokenInfo, ttl, TimeUnit.SECONDS);

        // Lưu lại vào status
        redisService.hSet(statusKey, FieldConst.SESSION_TOKEN, sessionToken);
      }
      redisService.expire(statusKey, ttl, TimeUnit.SECONDS);

    } else {
      // LẦN ĐẦU JOIN
      log.info("Student joining (first time): sessionExamId={}, student={}",
          sessionExamId, currentUser.getEmail());

      // Create session token
      sessionToken = generateSessionToken();
      StudentInfoDTO tokenInfo = StudentInfoDTO.fromUser(currentUser);
      String tokenKey = RedisUtils.examSessionToken(sessionExamId, sessionToken);
      redisService.set(tokenKey, tokenInfo, ttl, TimeUnit.SECONDS);

      // Thêm vào danh sách ĐÃ JOIN
      redisService.sAdd(joinedStudentsKey, studentIdStr);
      // Set TTL cho joined students set
      redisService.expire(joinedStudentsKey, ttl, TimeUnit.SECONDS);

      // Set student status
      Map<String, Object> statusData = new HashMap<>();
      statusData.put(FieldConst.STATUS, StudentExamStatus.JOINED.name());
      statusData.put(FieldConst.JOINED_AT, LocalDateTime.now());
      statusData.put(FieldConst.SESSION_TOKEN, sessionToken);
      statusData.put(FieldConst.STUDENT_CODE, currentUser.getCode());
      statusData.put(FieldConst.FULL_NAME, currentUser.getFullName());
      statusData.put(FieldConst.EMAIL, currentUser.getEmail());
      statusData.put(FieldConst.VIOLATIONS, 0);
      statusData.put(FieldConst.ANSWERED_COUNT, 0);
      redisService.hSetAll(statusKey, statusData);
      redisService.expire(statusKey, ttl, TimeUnit.SECONDS);

      redisService.hIncrement(RedisUtils.examSessionState(sessionExamId), FieldConst.JOINED_COUNT,
          1L);

      webSocketService.broadcastStudentJoined(
          sessionExamId,
          currentUser,
          LocalDateTime.now());
    }

    return JoinExamResponse.builder()
        .sessionToken(sessionToken)
        .sessionExamId(sessionExamId)
        .title(sessionExamState.getTitle())
        .description(sessionExamState.getDescription())
        .countdownStartAt(sessionExamState.getCountdownStartAt())
        .readyAt(sessionExamState.getReadyAt())
        .examEndAt(sessionExamState.getExamEndAt())
        .duration(sessionExamState.getDuration())
        .phase(phase)
        .serverTime(LocalDateTime.now())
        .build();
  }

  @Override
  @Transactional
  public DownloadExamResponse downloadExam(Long sessionExamId, String sessionToken) {
    // Verify token
    StudentInfoDTO studentInfo = this.verifySessionToken(sessionExamId, sessionToken);
    Long studentId = studentInfo.getStudentId();

    // Get and validate state
    SessionExamStateDTO state = getExamState(sessionExamId);
    SessionExamPhase phase = state.computePhase();

    if (!SessionExamPhase.ONGOING.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_NOT_READY,
          messageUtils.getMessage(MessageConst.EXAM_NOT_READY),
          HttpStatus.BAD_REQUEST);
    }

    // Get student status and validate not submitted
    Map<String, Object> statusData = getStudentStatus(sessionExamId, studentId);
    this.validateNotSubmitted(statusData);

    // Check if first download
    boolean isFirstDownload = !statusData.containsKey(FieldConst.DOWNLOADED_AT)
        || statusData.get(FieldConst.DOWNLOADED_AT) == null;

    // Get questions and saved answers
    List<ExamQuestionSnapshotResponse> examQuestionList = getOrGenerateQuestions(
        sessionExamId, studentId, state.getQuestionOrderMode());

    Map<Long, List<Long>> savedAnswers = loadSavedAnswers(sessionExamId, studentId);

    // Update if first download
    if (isFirstDownload) {
      this.handleFirstDownload(sessionExamId, studentId, studentInfo,
          statusData, examQuestionList);
    }

    // Build and return response
    return DownloadExamResponse.builder()
        .sessionExamId(sessionExamId)
        .title(state.getTitle())
        .description(state.getDescription())
        .duration(state.getDuration())
        .examEndAt(state.getExamEndAt())
        .serverTime(LocalDateTime.now())
        .questions(examQuestionList)
        .savedAnswers(savedAnswers)
        .build();
  }

  @Override
  @Transactional
  public SaveAnswersResponse saveAnswers(
      Long sessionExamId,
      String sessionToken,
      SaveAnswersRequest request) {

    // Verify token
    StudentInfoDTO studentInfo = verifySessionToken(sessionExamId, sessionToken);
    Long studentId = studentInfo.getStudentId();

    log.info("Student saving answers: sessionExamId={}, student={}, count={}",
        sessionExamId, studentInfo.getStudentCode(), request.getAnswers().size());

    // Get and validate state
    SessionExamStateDTO state = getExamState(sessionExamId);
    this.validatePhaseForSaving(state);

    // Get student status and validate not submitted
    Map<String, Object> statusData = getStudentStatus(sessionExamId, studentId);
    this.validateNotSubmitted(statusData);

    // Validate answers
    if (request.getAnswers().isEmpty()) {
      SaveAnswersResponse.builder()
              .savedAt(System.currentTimeMillis())
              .totalAnswered(0)
              .build();
    }
    this.validateAnswers(sessionExamId, request.getAnswers());

    // Save answers to Redis
    String answersKey = RedisUtils.studentAnswers(sessionExamId, studentId);
    this.saveAnswersToRedis(answersKey, request.getAnswers(), state.getExamEndAt());

    // Get total answered count
    int totalAnswered = redisService.hLen(answersKey).intValue();

    // Update student status
    this.updateStudentStatusAfterSave(sessionExamId, studentId, statusData, totalAnswered);

    log.info("Saved answers: sessionExamId={}, student={}, total={}",
        sessionExamId, studentInfo.getStudentCode(), totalAnswered);

    return SaveAnswersResponse.builder()
        .savedAt(System.currentTimeMillis())
        .totalAnswered(totalAnswered)
        .build();
  }

  @Override
  @Transactional
  public SubmitExamResponse submitExam(
      Long sessionExamId,
      String sessionToken,
      SubmitExamRequest request) {

    // Verify token
    StudentInfoDTO studentInfo = verifySessionToken(sessionExamId, sessionToken);
    Long studentId = studentInfo.getStudentId();

    log.info("Student submitting exam: sessionExamId={}, student={}",
        sessionExamId, studentInfo.getStudentCode());

    // Get and validate state
    SessionExamStateDTO state = getExamState(sessionExamId);
    this.validateSubmitPhase(state);

    // Get student status and validate not already submitted
    Map<String, Object> statusData = getStudentStatus(sessionExamId, studentId);
    this.validateNotSubmitted(statusData);

    // Load and grade student's answers
    Map<Long, List<Long>> studentAnswers = this.convertRequestToAnswerMap(request);
    GradingResult gradingResult = this.gradeExam(sessionExamId, studentAnswers);
    LocalDateTime submittedAt = LocalDateTime.now();

    // Update Redis and Database
    this.updateSubmissionData(sessionExamId, studentId, statusData,
        gradingResult, submittedAt, studentAnswers);

    // Broadcast submission event
    webSocketService.broadcastStudentSubmitted(
        sessionExamId, studentInfo, gradingResult.getScore(), submittedAt);

    log.info("Exam submitted: sessionExamId={}, student={}, score={}/10, correct={}/{}",
        sessionExamId, studentInfo.getStudentCode(),
        gradingResult.getScore(), gradingResult.getCorrectCount(),
        gradingResult.getTotalQuestions());

    // Return response
    return buildSubmitResponse(sessionExamId, studentId, submittedAt, gradingResult,
        state.isInstantlyResult());
  }

  @Override
  @Transactional
  public void endLiveSessionExam(SessionExam sessionExam) {

    // Get state from Redis
    String stateKey = RedisUtils.examSessionState(sessionExam.getSessionExamId());
    SessionExamStateDTO state = redisService.getExamState(stateKey);

    if (state == null) {
      throw new AppException(
          MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }

    SessionExamPhase phase = state.computePhase();
    if (!SessionExamPhase.ENDED.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_NOT_ENDED_YET,
          messageUtils.getMessage(MessageConst.EXAM_NOT_ENDED_YET),
          HttpStatus.BAD_REQUEST);
    }

    log.info("Starting batch grading: sessionExamId={}", sessionExam.getSessionExamId());

    // Get students từ Redis
    String eligibleStudentsKey = RedisUtils.examSessionEligibleStudents(
        sessionExam.getSessionExamId());
    Set<Object> studentIdSet = redisService.sMembers(eligibleStudentsKey);

    if (studentIdSet == null || studentIdSet.isEmpty()) {
      // Fallback to DB nếu Redis không có
      List<StudentSessionExam> studentsFromDb = studentSessionExamRepository
          .findAllBySessionExamId(sessionExam.getSessionExamId());
      studentIdSet = studentsFromDb.stream()
          .map(s -> (Object) s.getStudentId())
          .collect(Collectors.toSet());
    }

    Map<Long, User> userMap = userRepository.findAllByIdInAndIsDeletedFalse(
            studentIdSet.stream().map(e -> Long.parseLong(e.toString())).toList())
        .stream().collect(Collectors.toMap(User::getId, Function.identity()));

    // Tính điểm từng sinh viên và lưu kết quả
    for (Object studentIdObj : studentIdSet) {
      Long studentId = Long.parseLong(studentIdObj.toString());
      this.gradeStudent(sessionExam.getSessionExamId(), userMap.get(studentId));
    }

    // Update state
    redisService.hSet(stateKey, FieldConst.BATCH_GRADED_AT, LocalDateTime.now());

    SessionExamMonitoringResponse examMonitoring = this.buildMonitoringLogData(sessionExam);
    SessionExamMonitoringLog sessionExamMonitoringLog = SessionExamMonitoringLog.builder()
        .sessionExamId(sessionExam.getSessionExamId())
        .monitoringData(examMonitoring)
        .build();
    sessionExamMonitoringLogRepository.save(sessionExamMonitoringLog);

    sessionExam.setStatus(SessionExamStatus.ENDED);
    sessionExam.setEndScheduler(true);
    sessionExam.setEndSchedulerTime(LocalDateTime.now());
    sessionExamRepository.save(sessionExam);
    log.info("Batch grading completed: sessionExamId={}",sessionExam.getSessionExamId());
  }

  @Override
  public SessionExamMonitoringResponse getExamMonitoring(Long sessionExamId) {

    User currentUser = authService.getCurrentUser();

    // Validate instructor owns this exam
    SessionExam sessionExam = sessionExamRepository
        .findBySessionExamIdAndCreatedByAndIsDeletedFalse(sessionExamId, currentUser.getId())
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

    if (SessionExamStatus.NOT_STARTED.equals(sessionExam.getStatus())) {
      throw new AppException(
          MessageConst.EXAM_NOT_STARTED,
          messageUtils.getMessage(MessageConst.EXAM_NOT_STARTED),
          HttpStatus.BAD_REQUEST);
    }

    if (SessionExamStatus.ENDED.equals(sessionExam.getStatus())) {
      SessionExamMonitoringLog sessionExamMonitoringLog = sessionExamMonitoringLogRepository.findById(
              sessionExam.getSessionExamId())
          .orElseThrow(() -> new AppException(
              MessageConst.NOT_FOUND,
              messageUtils.getMessage(MessageConst.NOT_FOUND),
              HttpStatus.BAD_REQUEST));
      return sessionExamMonitoringLog.getMonitoringData();
    }
    // Get exam state
    String stateKey = RedisUtils.examSessionState(sessionExam.getSessionExamId());
    SessionExamStateDTO state = redisService.getExamState(stateKey);

    if (state == null) {
      log.warn("Session exam state {} not found", sessionExamId);
      return SessionExamMonitoringResponse.builder().build();
    }

    // Get all students' status
    List<StudentStateInfo> students = this.getStudentsMonitoringInfo(sessionExamId);

    List<ActivityHistoryResponse> activityHistories = this.getActivityHistory(sessionExamId);

    // Get active students
    Set<Long> activeStudentIds = this.getActiveStudents(sessionExamId);

    // Mark active status
    students.forEach(student -> {
      student.setActive(activeStudentIds.contains(student.getStudentId()));
    });

    // Compute phase
    SessionExamPhase phase = state.computePhase();

    return SessionExamMonitoringResponse.builder()
        .sessionExamId(sessionExamId)
        .title(state.getTitle())
        .description(state.getDescription())
        .phase(phase)
        .countdownStartAt(state.getCountdownStartAt())
        .readyAt(state.getReadyAt())
        .examEndAt(state.getExamEndAt())
        .duration(state.getDuration())
        .totalStudents(state.getTotalStudents())
        .joinedCount(state.getJoinedCount())
        .downloadedCount(state.getDownloadedCount())
        .submittedCount(state.getSubmittedCount())
        .violationCount(state.getViolationCount())
        .activeCount(activeStudentIds.size())
        .students(students)
        .activityHistories(activityHistories)
        .serverTime(LocalDateTime.now())
        .build();
  }

  /**
   * Lấy activity history từ Redis
   */
  private List<ActivityHistoryResponse> getActivityHistory(Long sessionExamId) {
    String activityHistoryKey = RedisUtils.sessionActivityHistory(sessionExamId);
    List<ActivityHistoryResponse> activityHistories = new ArrayList<>();
    List<Object> records = redisService.lRange(activityHistoryKey, 0, -1);
      if (records != null && !records.isEmpty()) {
        for (Object record : records) {
            ActivityHistoryRecordDTO dto = JsonUtils.parseFromJson(
                    record.toString(),
                    ActivityHistoryRecordDTO.class
            );
            activityHistories.add(ActivityHistoryResponse.fromDTO(sessionExamId, dto));
        }
      }

    return activityHistories;
  }
  private List<StudentStateInfo> getStudentsMonitoringInfo(Long sessionExamId) {

    // Get all eligible students
    String eligibleStudentsKey = RedisUtils.examSessionEligibleStudents(sessionExamId);
    Set<Object> studentIdSet = redisService.sMembers(eligibleStudentsKey);

    if (studentIdSet == null || studentIdSet.isEmpty()) {
      return new ArrayList<>();
    }
    Map<Long, User> userMap = userRepository.findAllByIdInAndIsDeletedFalse(
            studentIdSet.stream().map(e -> Long.parseLong(e.toString())).toList())
        .stream().collect(Collectors.toMap(User::getId, Function.identity()));

    List<StudentStateInfo> students = new ArrayList<>();

    for (Object studentIdObj : studentIdSet) {
      Long studentId = Long.parseLong(studentIdObj.toString());
       StudentStateInfo info = this.getStudentStateInfo(sessionExamId, userMap.get(studentId));
        students.add(info);
    }

    // Sort by status: SUBMITTED -> IN_PROGRESS -> DOWNLOADED -> JOINED -> NOT_STARTED
    students.sort((a, b) -> {
      int priorityA = this.getStatusPriority(a.getStatus());
      int priorityB = this.getStatusPriority(b.getStatus());
      if (priorityA != priorityB) {
        return Integer.compare(priorityA, priorityB);
      }
      // Same status, sort by violation count (desc)
      return Integer.compare(b.getViolationCount(), a.getViolationCount());
    });

    return students;
  }

  /**
   * Get monitoring info for a single student
   */
  private StudentStateInfo getStudentStateInfo(Long sessionExamId, User student) {

    String statusKey = RedisUtils.studentStatus(sessionExamId, student.getId());
    Map<String, Object> statusData = redisService.hGetAllAsString(statusKey);

    StudentStateInfo.StudentStateInfoBuilder builder = StudentStateInfo.builder()
        .studentId(student.getId())
        .studentCode(student.getCode())
        .fullName(student.getFullName())
        .email(student.getEmail());

    if (statusData.isEmpty()) {
      return builder
          .status(StudentExamStatus.NOT_JOINED)
          .violationCount(0)
          .answeredCount(0)
          .active(false)
          .build();
    }

    // Parse status data
    StudentExamStatus status = statusData.containsKey(FieldConst.STATUS)
        ? StudentExamStatus.fromString(statusData.get(FieldConst.STATUS).toString())
        : StudentExamStatus.JOINED;

    Integer violationCount = statusData.containsKey(FieldConst.VIOLATIONS)
        ? Integer.parseInt(statusData.get(FieldConst.VIOLATIONS).toString())
        : 0;

    Integer answeredCount = statusData.containsKey(FieldConst.ANSWERED_COUNT)
        ? Integer.parseInt(statusData.get(FieldConst.ANSWERED_COUNT).toString())
        : 0;

    LocalDateTime joinedAt = parseLocalDateTime(statusData.get(FieldConst.JOINED_AT));
    LocalDateTime downloadedAt = parseLocalDateTime(statusData.get(FieldConst.DOWNLOADED_AT));
    LocalDateTime submittedAt = parseLocalDateTime(statusData.get(FieldConst.SUBMITTED_AT));
    LocalDateTime lastHeartbeatAt = parseLocalDateTime(
        statusData.get(FieldConst.LAST_HEARTBEAT_AT));

    String lastViolationType = statusData.containsKey(FieldConst.LAST_VIOLATION_TYPE)
        ? statusData.get(FieldConst.LAST_VIOLATION_TYPE).toString()
        : null;

    LocalDateTime lastViolationAt = parseLocalDateTime(
        statusData.get(FieldConst.LAST_VIOLATION_AT));

    Double score = 0D;
    Integer correctCount = 0;

    if (StudentExamStatus.SUBMITTED.equals(status)) {
      score = statusData.containsKey(FieldConst.SCORE_KEY)
          ? Double.parseDouble(statusData.get(FieldConst.SCORE_KEY).toString())
          : null;

      correctCount = statusData.containsKey(FieldConst.CORRECT_COUNT)
          ? Integer.parseInt(statusData.get(FieldConst.CORRECT_COUNT).toString())
          : null;
    }

    return builder
        .status(status)
        .violationCount(violationCount)
        .answeredCount(answeredCount)
        .joinedAt(joinedAt)
        .downloadedAt(downloadedAt)
        .submittedAt(submittedAt)
        .lastHeartbeatAt(lastHeartbeatAt)
        .lastViolationType(lastViolationType)
        .lastViolationAt(lastViolationAt)
        .score(score)
        .correctCount(correctCount)
        .active(false)
        .build();
  }
  /**
   * Get active students (heartbeat trong 20s gần nhất)
   */
  private Set<Long> getActiveStudents(Long sessionExamId) {
    String pattern = String.format("exam:session:%d:active:*", sessionExamId);
    Set<String> keys = redisService.keys(pattern);
    if (keys == null || keys.isEmpty()) {
      return new HashSet<>();
    }

    return keys.stream()
        .map(
            key -> Long.parseLong(key.split(":")[4])) // exam:session:<sessionId>:active:<studentId>
        .collect(Collectors.toSet());
  }

  /**
   * Helper to get status priority for sorting
   */
  private int getStatusPriority(StudentExamStatus status) {
    return switch (status) {
      case SUBMITTED -> 1;
      case IN_PROGRESS -> 2;
      case DOWNLOADED -> 3;
      case JOINED -> 4;
      default -> 5;
    };
  }

  /**
   * Helper to parse LocalDateTime from Redis
   */
  private LocalDateTime parseLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    try {
      if (value instanceof LocalDateTime) {
        return (LocalDateTime) value;
      }
      return LocalDateTime.parse(value.toString());
    } catch (Exception e) {
      log.error("Error parsing LocalDateTime: {}", value, e);
      return null;
    }
  }


  private SessionExamContentDTO saveSessionExamContentToRedis(SessionExam sessionExam, long ttl) {
    // Lấy danh sách câu hỏi của đề thi
    List<ExamQuestionDTO> questionDTOS = this.getAllQuestions(
        sessionExam.getSessionExamId(), sessionExam.getQuestionOrderMode());

    SessionExamContentDTO sessionExamContentDTO = new SessionExamContentDTO();
    sessionExamContentDTO.setQuestions(questionDTOS);

    // Lưu nội dung đề thi
    redisService.set(
        RedisUtils.examSessionContent(sessionExam.getSessionExamId()),
        sessionExamContentDTO,
        ttl,
        TimeUnit.SECONDS
    );

    this.saveCorrectAnswersToRedis(sessionExam.getSessionExamId(), questionDTOS, ttl);

    return sessionExamContentDTO;
  }

  /**
   * Lưu đáp án đúng của đề thi vào Redis Format: Map<questionId, List<correctChoiceIds>>
   */
  private void saveCorrectAnswersToRedis(
      Long sessionExamId,
      List<ExamQuestionDTO> questions,
      long ttl) {

    String answerKeyRedis = RedisUtils.examSessionAnswerKey(sessionExamId);

    Map<String, List<Long>> answerKey = new HashMap<>();

    for (ExamQuestionDTO question : questions) {
      List<Long> correctChoices = question.getAnswers().stream()
          .filter(ExamQuestionAnswerDTO::getIsCorrect)
          .map(ExamQuestionAnswerDTO::getId)
          .collect(Collectors.toList());

      if (!correctChoices.isEmpty()) {
        answerKey.put(question.getId().toString(), correctChoices);
      }
    }

    // Lưu vào Redis
    for (Map.Entry<String, List<Long>> entry : answerKey.entrySet()) {
      redisService.hSet(answerKeyRedis, entry.getKey(), entry.getValue());
    }

    // Set TTL
    redisService.expire(answerKeyRedis, ttl, TimeUnit.SECONDS);

    log.info("Saved answer key for sessionExamId={}: {} questions",
        sessionExamId, answerKey.size());
  }

  private void gradeStudent(Long sessionExamId, User student) {

    String statusKey = RedisUtils.studentStatus(sessionExamId, student.getId());
    Map<String, Object> statusData = redisService.hGetAllAsString(statusKey);

    // Check current status
    if (!statusData.containsKey(FieldConst.STATUS)) {
      this.updateNotStartedInDatabase(sessionExamId, student.getId());
      return;
    }

    StudentExamStatus currentStatus = StudentExamStatus.fromString(
        statusData.get(FieldConst.STATUS).toString());

    // ĐÃ SUBMIT
    if (StudentExamStatus.SUBMITTED.equals(currentStatus)) {
      return;
    }

    // Chỉ JOIN, chưa download đề
    if (StudentExamStatus.JOINED.equals(currentStatus)) {
      this.updateNotStartedInDatabase(sessionExamId, student.getId());
      return;
    }

    // ĐÃ DOWNLOAD đề
    Map<Long, List<Long>> studentAnswers = loadSavedAnswers(sessionExamId, student.getId());

    // Check có đáp án không
    if (studentAnswers.isEmpty()) {
      this.updateNotSubmittedInDatabase(sessionExamId, student.getId());
      return;
    }

    GradingResult gradingResult = this.gradeExam(sessionExamId, studentAnswers);
    LocalDateTime submittedAt = LocalDateTime.now();

    // Update student status
    statusData.put(FieldConst.STATUS, StudentExamStatus.SUBMITTED.name());
    statusData.put(FieldConst.SUBMITTED_AT, submittedAt);
    statusData.put(FieldConst.SCORE_KEY, gradingResult.getScore());
    statusData.put(FieldConst.CORRECT_COUNT, gradingResult.getCorrectCount());
    statusData.put(FieldConst.TOTAL_QUESTIONS, gradingResult.getTotalQuestions());
    statusData.put(FieldConst.AUTO_GRADED, true);
    redisService.hSetAll(statusKey, statusData);

    // Increment submitted count
    String stateKey = RedisUtils.examSessionState(sessionExamId);
    redisService.hIncrement(stateKey, FieldConst.SUBMITTED_COUNT, 1L);

    // Save to database
    this.saveSubmissionToDatabase(
        sessionExamId,
        student.getId(),
        gradingResult.getScore(),
        gradingResult.getCorrectCount(),
        gradingResult.getTotalQuestions(),
        submittedAt,
        studentAnswers,
        gradingResult.getCorrectnessMap()
    );

    log.info("Auto-graded student: sessionExamId={}, student={}, score={}/10, answered={}/{}",
        sessionExamId, student.getCode(), gradingResult.getScore(),
        studentAnswers.size(), gradingResult.getTotalQuestions());
  }

  /**
   * Update submission data in both Redis and Database
   */
  private void updateSubmissionData(
      Long sessionExamId,
      Long studentId,
      Map<String, Object> statusData,
      GradingResult gradingResult,
      LocalDateTime submittedAt,
      Map<Long, List<Long>> studentAnswers) {

    // Update Redis status
    updateRedisSubmissionStatus(sessionExamId, studentId, statusData,
        gradingResult, submittedAt);

    // Save to database
    saveSubmissionToDatabase(sessionExamId, studentId, gradingResult,
        submittedAt, studentAnswers, false);
  }

  /**
   * Overload method - backward compatibility
   */
  private void saveSubmissionToDatabase(
      Long sessionExamId,
      Long studentId,
      double score,
      int correctCount,
      int totalQuestions,
      LocalDateTime submittedAt,
      Map<Long, List<Long>> studentAnswers,
      Map<Long, Boolean> correctnessMap) {

    // Convert to GradingResult
    GradingResult gradingResult = GradingResult.builder()
        .score(score)
        .correctCount(correctCount)
        .totalQuestions(totalQuestions)
        .correctnessMap(correctnessMap)
        .build();

    // Call main method
    saveSubmissionToDatabase(sessionExamId, studentId, gradingResult,
        submittedAt, studentAnswers, true);
  }

  /**
   * Update Redis with submission status
   */
  private void updateRedisSubmissionStatus(
      Long sessionExamId,
      Long studentId,
      Map<String, Object> statusData,
      GradingResult gradingResult,
      LocalDateTime submittedAt) {

    String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);

    statusData.put(FieldConst.STATUS, StudentExamStatus.SUBMITTED.name());
    statusData.put(FieldConst.SUBMITTED_AT, submittedAt);
    statusData.put(FieldConst.SCORE_KEY, gradingResult.getScore());
    statusData.put(FieldConst.CORRECT_COUNT, gradingResult.getCorrectCount());
    statusData.put(FieldConst.TOTAL_QUESTIONS, gradingResult.getTotalQuestions());
    redisService.hSetAll(statusKey, statusData);

    // Increment submitted count
    String stateKey = RedisUtils.examSessionState(sessionExamId);
    redisService.hIncrement(stateKey, FieldConst.SUBMITTED_COUNT, 1L);
  }

  /**
   * Build submit response
   */
  private SubmitExamResponse buildSubmitResponse(
      Long sessionExamId,
      Long studentId,
      LocalDateTime submittedAt,
      GradingResult gradingResult,
      boolean isInstantlyResult) {

    return SubmitExamResponse.builder()
        .sessionExamId(sessionExamId)
        .studentId(studentId)
        .submittedAt(submittedAt)
        .score(isInstantlyResult ? gradingResult.getScore() : null)
        .correctCount(gradingResult.getCorrectCount())
        .totalQuestions(gradingResult.getTotalQuestions())
        .build();
  }

  private void saveSubmissionToDatabase(
      Long sessionExamId,
      Long studentId,
      GradingResult gradingResult,
      LocalDateTime submittedAt,
      Map<Long, List<Long>> studentAnswers,
      boolean autoGraded) {

    // Get StudentSessionExam record
    StudentSessionExam studentSessionExam = getStudentSessionExam(sessionExamId, studentId);

    // Load correct answers
    String answerKeyRedis = RedisUtils.examSessionAnswerKey(sessionExamId);
    Map<Object, Object> rawAnswerKey = redisService.hGetAll(answerKeyRedis);

    if (rawAnswerKey == null || rawAnswerKey.isEmpty()) {
      throw new AppException(
          MessageConst.ANSWER_KEY_NOT_FOUND,
          messageUtils.getMessage(MessageConst.ANSWER_KEY_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }
    Map<Long, List<Long>> answerKey = new HashMap<>();

    for (Map.Entry<Object, Object> entry : rawAnswerKey.entrySet()) {
      Long questionId = Long.valueOf(entry.getKey().toString());
      @SuppressWarnings("unchecked")
      List<Long> correctChoiceIds = (List<Long>) entry.getValue();
      answerKey.put(questionId, correctChoiceIds);
    }
    // Get or create submission DTO with questions
    SessionExamSubmissionDTO submissionDTO = getOrCreateSubmissionDTO(
            studentSessionExam, answerKey, sessionExamId, studentId);
    submissionDTO.setStudentAnswers(studentAnswers);

    // Update entity with submission data
    updateStudentSessionExamEntity(studentSessionExam, gradingResult,
        submittedAt, submissionDTO, autoGraded);

    // Save to database
    studentSessionExamRepository.save(studentSessionExam);

    log.info(
        "Saved submission to database: sessionExamId={}, studentId={}, score={}, autoGraded={}",
        sessionExamId, studentId, gradingResult.getScore(), autoGraded);
  }

  /**
   * Get StudentSessionExam entity
   */
  private StudentSessionExam getStudentSessionExam(Long sessionExamId, Long studentId) {
    return studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, studentId)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));
  }

  /**
   * Get or create submission DTO
   */
  private SessionExamSubmissionDTO getOrCreateSubmissionDTO(
      StudentSessionExam studentSessionExam,
      Map<Long, List<Long>> answerKey,
      Long sessionExamId,
      Long studentId) {

    SessionExamSubmissionDTO dto;

    if (studentSessionExam.getSubmissionResult() != null) {
      dto = JsonUtils.parseFromJson(
              studentSessionExam.getSubmissionResult(),
              SessionExamSubmissionDTO.class);

      if (dto == null) {
        dto = SessionExamSubmissionDTO.builder()
                .questions(loadQuestionsForStudent(sessionExamId, studentId))
                .build();
      }
    } else {
      dto = SessionExamSubmissionDTO.builder()
              .questions(loadQuestionsForStudent(sessionExamId, studentId))
              .build();
    }

    if (dto.getQuestions() != null) {
      for (ExamQuestionDTO question : dto.getQuestions()) {
        List<Long> correctChoiceIds = answerKey.getOrDefault(question.getId(), List.of());

        for (ExamQuestionAnswerDTO answerDTO : question.getAnswers()) {
          boolean isCorrect = correctChoiceIds.contains(answerDTO.getId());
          answerDTO.setIsCorrect(isCorrect);
        }
      }
    }
    return dto;
  }

  /**
   * Update StudentSessionExam entity with submission data
   */
  private void updateStudentSessionExamEntity(
      StudentSessionExam entity,
      GradingResult gradingResult,
      LocalDateTime submittedAt,
      SessionExamSubmissionDTO submissionDTO,
      boolean autoGraded) {

    entity.setSubmissionStatus(ExamSubmissionStatus.SUBMITTED);
    entity.setSubmissionTime(submittedAt);
    entity.setScore(gradingResult.getScore());
    entity.setCorrectCount(gradingResult.getCorrectCount());
    entity.setTotalQuestions(gradingResult.getTotalQuestions());
    entity.setSubmissionResult(JsonUtils.convertToJson(submissionDTO));
    entity.setAutoGraded(autoGraded);
  }

  /**
   * Load questions for a student
   */
  private List<ExamQuestionDTO> loadQuestionsForStudent(
      Long sessionExamId,
      Long studentId) {

    // Try to get from student-specific cache first (for RANDOM mode)
    String studentContentKey = RedisUtils.examSessionStudentContent(sessionExamId, studentId);
    SessionExamContentDTO studentContent = redisService.get(
        studentContentKey,
        SessionExamContentDTO.class
    );

    if (studentContent != null && studentContent.getQuestions() != null) {
      return studentContent.getQuestions();
    }

    // Fall back to shared content
    String contentKey = RedisUtils.examSessionContent(sessionExamId);
    SessionExamContentDTO examContent = redisService.get(
        contentKey,
        SessionExamContentDTO.class
    );

    if (examContent == null || examContent.getQuestions() == null) {
      throw new AppException(
          MessageConst.EXAM_CONTENT_NOT_FOUND,
          messageUtils.getMessage(MessageConst.EXAM_CONTENT_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }

    return examContent.getQuestions();
  }

  /**
   * @param duration minutes
   * @return duration seconds
   */
  private long toSeconds(Long duration) {
    if (duration == null) {
      return 0;
    }
    return duration * 60;
  }

  private String generateSessionToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Verify session token
   */
  private StudentInfoDTO verifySessionToken(Long sessionExamId, String sessionToken) {
    String tokenKey = RedisUtils.examSessionToken(sessionExamId, sessionToken);
    StudentInfoDTO tokenInfo = redisService.get(tokenKey, StudentInfoDTO.class);

    if (tokenInfo == null) {
      throw new AppException(
          MessageConst.INVALID_EXAM_TOKEN,
          messageUtils.getMessage(MessageConst.INVALID_EXAM_TOKEN),
          HttpStatus.BAD_REQUEST);
    }

    return tokenInfo;
  }

  /**
   * Lấy hoặc tạo questions dựa trên order mode - RANDOM: Cache riêng mỗi SV, lấy cache nếu đã có -
   * Các mode khác: Cache chung, lấy từ base content
   */
  private List<ExamQuestionSnapshotResponse> getOrGenerateQuestions(
      Long sessionExamId,
      Long studentId,
      QuestionOrderMode orderMode) {

    if (QuestionOrderMode.RANDOM.equals(orderMode)) {
      return this.getOrGenerateRandomQuestions(sessionExamId, studentId);
    }

    // Sequential or other modes - use shared content
    SessionExamContentDTO baseContent = getBaseExamContent(sessionExamId);

    log.info("Using shared content (mode: {}): {} questions",
        orderMode, baseContent.getQuestions().size());

    return baseContent.getQuestions().stream()
        .map(ExamQuestionSnapshotResponse::fromDTO)
        .toList();
  }

  /**
   * Get or generate random questions for a student
   */
  private List<ExamQuestionSnapshotResponse> getOrGenerateRandomQuestions(
      Long sessionExamId,
      Long studentId) {

    // Check cache riêng của SV
    String studentContentKey = RedisUtils.examSessionStudentContent(sessionExamId, studentId);
    SessionExamContentDTO cachedContent = redisService.get(
        studentContentKey,
        SessionExamContentDTO.class);

    if (cachedContent != null && cachedContent.getQuestions() != null) {
      log.debug("Using cached random questions for student {}", studentId);
      return cachedContent.getQuestions().stream()
          .map(ExamQuestionSnapshotResponse::fromDTO)
          .toList();
    }

    // Generate new random order
    SessionExamContentDTO baseContent = getBaseExamContent(sessionExamId);
    List<ExamQuestionSnapshotResponse> shuffled = new ArrayList<>(
        baseContent.getQuestions().stream()
            .map(ExamQuestionSnapshotResponse::fromDTO)
            .toList());
    Collections.shuffle(shuffled);

    // Save cache riêng cho SV này
    SessionExamContentDTO studentContent = new SessionExamContentDTO();
    studentContent.setQuestions(shuffled.stream().map(ExamQuestionDTO::fromResponse).toList());
    redisService.set(studentContentKey, studentContent, 24, TimeUnit.HOURS);

    log.info("Generated and cached random questions for student {}: {} questions",
        studentId, shuffled.size());

    return shuffled;
  }

  /**
   * Validate student answers using cached validation map
   */
  private void validateAnswers(Long sessionExamId, List<StudentAnswerRequest> answers) {
    // Get cached validation map
    String validationKey = RedisUtils.examSessionValidation(sessionExamId);
    Map<Long, Set<Long>> validChoicesMap = redisService.get(validationKey,
        new TypeReference<Map<Long, Set<Long>>>() {
        });

    if (validChoicesMap == null) {
      throw new AppException(
          MessageConst.EXAM_CONTENT_NOT_FOUND,
          messageUtils.getMessage(MessageConst.EXAM_CONTENT_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }

    // Validate từng answer
    for (StudentAnswerRequest answer : answers) {
      Long questionId = answer.getQuestionSnapshotId();

      // Check questionId có trong đề không
      if (!validChoicesMap.containsKey(questionId)) {
        throw new AppException(
            MessageConst.INVALID_QUESTION,
            messageUtils.getMessage(MessageConst.INVALID_QUESTION),
            HttpStatus.BAD_REQUEST);
      }

      // Check tất cả selectedChoices có hợp lệ không
      Set<Long> validChoices = validChoicesMap.get(questionId);

      for (Long choiceId : answer.getSelectedAnswerIds()) {
        if (!validChoices.contains(choiceId)) {
          throw new AppException(
              MessageConst.INVALID_CHOICE,
              messageUtils.getMessage(MessageConst.INVALID_CHOICE),
              HttpStatus.BAD_REQUEST);
        }
      }
    }

    log.debug("Validated {} answers successfully", answers.size());
  }

  private Map<Long, List<Long>> loadSavedAnswers(Long sessionExamId, Long studentId) {
    String answersKey = RedisUtils.studentAnswers(sessionExamId, studentId);
    Map<Object, Object> rawAnswers = redisService.hGetAll(answersKey);

    if (rawAnswers == null || rawAnswers.isEmpty()) {
      log.debug("No saved answers found for student {}", studentId);
      return new HashMap<>();
    }

    Map<Long, List<Long>> result = new HashMap<>();

    for (Map.Entry<Object, Object> entry : rawAnswers.entrySet()) {
      try {
        Long questionId = Long.parseLong(entry.getKey().toString());
        Object value = entry.getValue();
        List<Long> selectedChoices;

        if (value instanceof List) {
          selectedChoices = ((List<?>) value).stream()
              .map(obj -> {
                if (obj instanceof Number) {
                  return ((Number) obj).longValue();
                }
                return Long.parseLong(obj.toString());
              })
              .collect(Collectors.toList());
        } else {
          log.warn("Unexpected answer format for questionId {}: {}",
              questionId, value.getClass());
          continue;
        }

        if (!selectedChoices.isEmpty()) {
          result.put(questionId, selectedChoices);
        }

      } catch (Exception e) {
        log.error("Error parsing answer for questionId {}: {}",
            entry.getKey(), e.getMessage());
      }
    }

    log.info("Loaded {} saved answers for student {}", result.size(), studentId);

    return result;
  }

  private void saveValidationMapToRedis(
      Long sessionExamId,
      SessionExamContentDTO examContent,
      long ttl) {

    Map<Long, Set<Long>> validChoicesMap = new HashMap<>();

    for (ExamQuestionDTO question : examContent.getQuestions()) {
      Set<Long> validChoiceIds = question.getAnswers().stream()
          .map(ExamQuestionAnswerDTO::getId)
          .collect(Collectors.toSet());
      validChoicesMap.put(question.getId(), validChoiceIds);
    }

    String validationKey = RedisUtils.examSessionValidation(sessionExamId);
    redisService.set(validationKey, validChoicesMap, ttl, TimeUnit.SECONDS);

    log.info("Saved validation map for sessionExamId={}: {} questions",
        sessionExamId, validChoicesMap.size());
  }

  /**
   * Validate phase for submission Allow to submit during ONGOING or within buffer time after ENDED
   */
  private void validateSubmitPhase(SessionExamStateDTO state) {
    SessionExamPhase phase = state.computePhase();

    if (SessionExamPhase.NOT_STARTED.equals(phase) ||
        SessionExamPhase.COUNTDOWN.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_NOT_STARTED,
          messageUtils.getMessage(MessageConst.EXAM_NOT_STARTED),
          HttpStatus.BAD_REQUEST);
    }

    if (SessionExamPhase.ENDED.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_ENDED,
          messageUtils.getMessage(MessageConst.EXAM_ENDED),
          HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Grade exam based on student answers
   *
   * @return GradingResult containing score and correctness details
   */
  private GradingResult gradeExam(Long sessionExamId, Map<Long, List<Long>> studentAnswers) {
    // Load correct answers
    String answerKeyRedis = RedisUtils.examSessionAnswerKey(sessionExamId);
    Map<Object, Object> rawAnswerKey = redisService.hGetAll(answerKeyRedis);

    if (rawAnswerKey == null || rawAnswerKey.isEmpty()) {
      throw new AppException(
          MessageConst.ANSWER_KEY_NOT_FOUND,
          messageUtils.getMessage(MessageConst.ANSWER_KEY_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }

    // Chấm từng câu
    Map<Long, Boolean> correctnessMap = new HashMap<>();
    int correctCount = 0;
    int totalQuestions = rawAnswerKey.size();

    for (Map.Entry<Object, Object> entry : rawAnswerKey.entrySet()) {
      Long questionId = Long.parseLong(entry.getKey().toString());

      List<Long> correctChoices = Utils.parseChoiceList(entry.getValue());
      List<Long> studentChoices = studentAnswers.getOrDefault(questionId, new ArrayList<>());

      // So sánh đáp án
      boolean isCorrect = new HashSet<>(correctChoices).equals(new HashSet<>(studentChoices));
      correctnessMap.put(questionId, isCorrect);

      if (isCorrect) {
        correctCount++;
      }
    }

    // Tính điểm
    double score = (double) correctCount / totalQuestions * 10.0;
    score = Math.round(score * 100.0) / 100.0;

    log.debug("Grading completed: correct={}/{}, score={}/10",
        correctCount, totalQuestions, score);

    return GradingResult.builder()
        .score(score)
        .correctCount(correctCount)
        .totalQuestions(totalQuestions)
        .correctnessMap(correctnessMap)
        .build();
  }

  /**
   * Get base exam content from Redis
   */
  private SessionExamContentDTO getBaseExamContent(Long sessionExamId) {
    String contentKey = RedisUtils.examSessionContent(sessionExamId);
    SessionExamContentDTO content = redisService.get(contentKey, SessionExamContentDTO.class);

    if (content == null || content.getQuestions() == null) {
      throw new AppException(
          MessageConst.EXAM_CONTENT_NOT_FOUND,
          messageUtils.getMessage(MessageConst.EXAM_CONTENT_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
    }

    return content;
  }

  /**
   * Validate student has not submitted
   */
  private void validateNotSubmitted(Map<String, Object> statusData) {
    if (statusData.containsKey(FieldConst.STATUS)) {
      StudentExamStatus currentStatus = StudentExamStatus.fromString(
          statusData.get(FieldConst.STATUS).toString());

      if (StudentExamStatus.SUBMITTED.equals(currentStatus)) {
        throw new AppException(
            MessageConst.EXAM_ALREADY_SUBMITTED,
            messageUtils.getMessage(MessageConst.EXAM_ALREADY_SUBMITTED),
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  /**
   * Get student status from Redis
   */
  private Map<String, Object> getStudentStatus(Long sessionExamId, Long studentId) {
    String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
    return redisService.hGetAllAsString(statusKey);
  }

  /**
   * Handle first download - update Redis, DB, and broadcast
   */
  private void handleFirstDownload(
      Long sessionExamId,
      Long studentId,
      StudentInfoDTO studentInfo,
      Map<String, Object> statusData,
      List<ExamQuestionSnapshotResponse> questions) {

    LocalDateTime now = LocalDateTime.now();

    // Update Redis status
    String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
    statusData.put(FieldConst.STATUS, StudentExamStatus.DOWNLOADED.name());
    statusData.put(FieldConst.DOWNLOADED_AT, now);
    redisService.hSetAll(statusKey, statusData);

    // Increment downloaded count
    String stateKey = RedisUtils.examSessionState(sessionExamId);
    redisService.hIncrement(stateKey, FieldConst.DOWNLOADED_COUNT, 1L);

    // Update database
    this.saveExamQuestionsToDatabase(sessionExamId, studentId, questions, now);

    // Broadcast WebSocket
    webSocketService.broadcastStudentDownloaded(sessionExamId, studentInfo, now);

    log.info("First download completed: sessionExamId={}, studentId={}, questions={}",
        sessionExamId, studentId, questions.size());
  }

  /**
   * Save exam questions to database (first download)
   */
  private void saveExamQuestionsToDatabase(
      Long sessionExamId,
      Long studentId,
      List<ExamQuestionSnapshotResponse> questions,
      LocalDateTime downloadedAt) {

    StudentSessionExam studentSessionExam = studentSessionExamRepository
        .findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, studentId)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

    // Build submission DTO
    SessionExamSubmissionDTO submissionDTO = SessionExamSubmissionDTO.builder()
        .questions(questions.stream().map(ExamQuestionDTO::fromResponse).toList())
        .build();

    // Update entity
    studentSessionExam.setSubmissionStatus(ExamSubmissionStatus.NOT_SUBMITTED);
    studentSessionExam.setDownloadedAt(downloadedAt);
    studentSessionExam.setExamStartTime(downloadedAt);
    studentSessionExam.setSubmissionResult(JsonUtils.convertToJson(submissionDTO));
    studentSessionExam.setTotalQuestions(questions.size());

    studentSessionExamRepository.save(studentSessionExam);
  }

  /**
   * Validate phase is suitable for saving answers
   */
  private void validatePhaseForSaving(SessionExamStateDTO state) {
    SessionExamPhase phase = state.computePhase();

    if (SessionExamPhase.NOT_STARTED.equals(phase) ||
        SessionExamPhase.COUNTDOWN.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_NOT_STARTED,
          messageUtils.getMessage(MessageConst.EXAM_NOT_STARTED),
          HttpStatus.BAD_REQUEST);
    }

    if (SessionExamPhase.ENDED.equals(phase)) {
      throw new AppException(
          MessageConst.EXAM_ENDED,
          messageUtils.getMessage(MessageConst.EXAM_ENDED),
          HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Save answers to Redis
   */
  private void saveAnswersToRedis(
      String answersKey,
      List<StudentAnswerRequest> answers,
      LocalDateTime examEndAt) {

    // Save each answer
    for (StudentAnswerRequest answer : answers) {
      String questionIdStr = answer.getQuestionSnapshotId().toString();
      redisService.hSet(answersKey, questionIdStr, answer.getSelectedAnswerIds());
    }

    // Set TTL
    long ttl = Duration.between(LocalDateTime.now(), examEndAt)
        .getSeconds() + AppConst.TTL_BUFFER;
    redisService.expire(answersKey, ttl, TimeUnit.SECONDS);
  }

  /**
   * Update student status after saving answers
   */
  private void updateStudentStatusAfterSave(
      Long sessionExamId,
      Long studentId,
      Map<String, Object> statusData,
      int totalAnswered) {

    long now = System.currentTimeMillis();

    // Update Redis status
    String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
    statusData.put(FieldConst.STATUS, StudentExamStatus.IN_PROGRESS.name());
    statusData.put(FieldConst.LAST_SAVE_AT, now);
    statusData.put(FieldConst.ANSWERED_COUNT, totalAnswered);
    redisService.hSetAll(statusKey, statusData);
  }

  /**
   * Save exam state to Redis as flat hash structure Allows individual field increment/decrement
   */
  private void saveExamStateToRedis(
      String stateKey,
      SessionExamStateDTO state,
      long ttl) {

    Map<String, Object> stateMap = new HashMap<>();
    stateMap.put(FieldConst.INSTRUCTOR_ID, state.getInstructorId());
    stateMap.put(FieldConst.SESSION_EXAM_ID_KEY, state.getSessionExamId());
    stateMap.put(FieldConst.TITLE, state.getTitle());
    stateMap.put(FieldConst.DESCRIPTION, state.getDescription());
    stateMap.put(FieldConst.QUESTION_ORDER_MODE, state.getQuestionOrderMode().name());
    stateMap.put(FieldConst.IS_INSTANTLY_RESULT, state.isInstantlyResult());
    stateMap.put(FieldConst.COUNTDOWN_START_AT, state.getCountdownStartAt());
    stateMap.put(FieldConst.DURATION, state.getDuration());
    stateMap.put(FieldConst.TOTAL_STUDENTS, state.getTotalStudents());
    stateMap.put(FieldConst.JOINED_COUNT, state.getJoinedCount());
    stateMap.put(FieldConst.DOWNLOADED_COUNT, state.getDownloadedCount());
    stateMap.put(FieldConst.SUBMITTED_COUNT, state.getSubmittedCount());
    stateMap.put(FieldConst.VIOLATION_COUNT, state.getViolationCount());
    stateMap.put(FieldConst.READY_AT, state.getReadyAt());
    stateMap.put(FieldConst.EXAM_END_AT, state.getExamEndAt());

    // Save as flat hash
    redisService.hSetAll(stateKey, stateMap);

    // Set TTL
    redisService.expire(stateKey, ttl, TimeUnit.SECONDS);

    log.info("Saved exam state to Redis (flat structure): {}", stateKey);
  }

  /**
   * Get exam state from Redis
   */
  private SessionExamStateDTO getExamState(Long sessionExamId) {
    String stateKey = RedisUtils.examSessionState(sessionExamId);
    return redisService.getExamState(stateKey);
  }

  /**
   * Convert SubmitExamRequest to answer map
   */
  private Map<Long, List<Long>> convertRequestToAnswerMap(SubmitExamRequest request) {
    Map<Long, List<Long>> result = new HashMap<>();

    if (request == null || request.getAnswers() == null) {
      return result;
    }

    for (StudentAnswerRequest answer : request.getAnswers()) {
      if (answer.getQuestionSnapshotId() != null &&
          answer.getSelectedAnswerIds() != null &&
          !answer.getSelectedAnswerIds().isEmpty()) {

        result.put(answer.getQuestionSnapshotId(), answer.getSelectedAnswerIds());
      }
    }

    return result;
  }

  /**
   * Update NOT_SUBMITTED status in database
   */
  private void updateNotSubmittedInDatabase(Long sessionExamId, Long studentId) {
    StudentSessionExam entity = this.getStudentSessionExam(sessionExamId, studentId);
    entity.setSubmissionStatus(ExamSubmissionStatus.NOT_SUBMITTED);
    entity.setScore(0D);
    entity.setCorrectCount(0);
    entity.setTotalQuestions(0);
    entity.setSubmissionTime(null);
    entity.setAutoGraded(true);

    studentSessionExamRepository.save(entity);
  }

  /**
   * Update NOT_STARTED status in database
   */
  private void updateNotStartedInDatabase(Long sessionExamId, Long studentId) {
    StudentSessionExam entity = this.getStudentSessionExam(sessionExamId, studentId);
    entity.setSubmissionStatus(ExamSubmissionStatus.NOT_STARTED);
    entity.setScore(0D);
    entity.setCorrectCount(0);
    entity.setTotalQuestions(0);
    entity.setSubmissionTime(null);
    entity.setAutoGraded(true);

    studentSessionExamRepository.save(entity);
  }

  @Override
  public SessionExamDescriptiveStatisticResponse getDescriptiveStatistic(Long sessionExamId) {
    log.info("Getting descriptive statistics for session exam {}", sessionExamId);

    // Kiểm tra session exam tồn tại
    sessionExamRepository.findById(sessionExamId)
        .orElseThrow(() -> new AppException(
            MessageConst.NOT_FOUND,
            messageUtils.getMessage(MessageConst.NOT_FOUND),
            HttpStatus.BAD_REQUEST));

    // Lấy tất cả scores đã nộp bài (có điểm)
    List<Double> scores = studentSessionExamRepository.findAllScoresBySessionExamId(sessionExamId);

    // Lấy thông tin tổng số sinh viên
    Long totalStudents = studentSessionExamRepository.countTotalStudentsBySessionExamId(
        sessionExamId);
    Long submittedStudents = studentSessionExamRepository.countSubmittedStudentsBySessionExamId(
        sessionExamId, ExamSubmissionStatus.SUBMITTED);
    int notSubmittedStudents = totalStudents.intValue() - submittedStudents.intValue();

    // Nếu không có điểm nào, trả về thống kê rỗng
    if (scores.isEmpty()) {
      return SessionExamDescriptiveStatisticResponse.builder()
          .mean(null)
          .median(null)
          .max(null)
          .min(null)
          .mode(null)
          .scoreDistribution(createEmptyScoreDistribution())
          .totalStudents(totalStudents.intValue())
          .submittedStudents(submittedStudents.intValue())
          .notSubmittedStudents(notSubmittedStudents)
          .build();
    }

    // Tính toán các thống kê
    Double mean = calculateMean(scores);
    Double median = calculateMedian(scores);
    Double max = scores.get(scores.size() - 1); // Đã được sắp xếp tăng dần
    Double min = scores.get(0);
    Double mode = calculateMode(scores);
    List<SessionExamDescriptiveStatisticResponse.ScoreDistributionItem> scoreDistribution =
        calculateScoreDistribution(scores);

    log.info("Calculated statistics: mean={}, median={}, max={}, min={}, mode={}, totalStudents={}",
        mean, median, max, min, mode, totalStudents);

    return SessionExamDescriptiveStatisticResponse.builder()
        .mean(mean)
        .median(median)
        .max(max)
        .min(min)
        .mode(mode)
        .scoreDistribution(scoreDistribution)
        .totalStudents(totalStudents.intValue())
        .submittedStudents(submittedStudents.intValue())
        .notSubmittedStudents(notSubmittedStudents)
        .build();
  }

  @Override
  public StudentExamResultResponse getStudentExamResult(Long studentSessionExamId) {
    User user = authService.getCurrentUser();
    StudentExamResultQueryDTO queryDTO = studentSessionExamRepository.getStudentExamResult(
        studentSessionExamId, user.getId()
    );
    if (queryDTO == null) {
      throw new AppException(AppConst.MessageConst.NOT_FOUND,
          messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }
      SessionExamSubmissionDTO sessionExamSubmissionDTO = JsonUtils.parseFromJson(
          queryDTO.getSubmissionResult(), SessionExamSubmissionDTO.class);
      if (sessionExamSubmissionDTO == null) {
        throw new AppException(AppConst.MessageConst.NOT_FOUND,
            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
      }
    Map<Long, List<Long>> studentAnswers = sessionExamSubmissionDTO.getStudentAnswers();
    List<StudentExamQuestionResponse> questions = sessionExamSubmissionDTO.getQuestions().stream()
          .map(StudentExamQuestionResponse::fromDTO)
          .toList();

    return StudentExamResultResponse.builder()
        .studentSessionExamId(queryDTO.getStudentSessionExamId())
        .score(queryDTO.getScore())
        .questions(questions)
        .studentAnswers(studentAnswers)
        .build();
  }

  /**
   * Tính điểm trung bình
   */
  private Double calculateMean(List<Double> scores) {
    if (scores.isEmpty()) {
      return null;
    }
    double sum = scores.stream().mapToDouble(Double::doubleValue).sum();
    return sum / scores.size();
  }

  /**
   * Tính trung vị
   */
  private Double calculateMedian(List<Double> scores) {
    if (scores.isEmpty()) {
      return null;
    }
    int size = scores.size();
    if (size % 2 == 0) {
      // Số chẵn phần tử: lấy trung bình của 2 phần tử giữa
      return (scores.get(size / 2 - 1) + scores.get(size / 2)) / 2.0;
    } else {
      // Số lẻ phần tử: lấy phần tử giữa
      return scores.get(size / 2);
    }
  }

  /**
   * Tính mode (điểm xuất hiện nhiều nhất) Trả về null nếu không có mode duy nhất hoặc tất cả điểm
   * đều xuất hiện 1 lần
   */
  private Double calculateMode(List<Double> scores) {
    if (scores.isEmpty()) {
      return null;
    }

    // Đếm tần suất của mỗi điểm
    Map<Double, Integer> frequencyMap = new HashMap<>();
    for (Double score : scores) {
      frequencyMap.put(score, frequencyMap.getOrDefault(score, 0) + 1);
    }

    // Tìm tần suất cao nhất
    int maxFrequency = frequencyMap.values().stream()
        .mapToInt(Integer::intValue)
        .max()
        .orElse(0);

    // Nếu tất cả điểm đều xuất hiện 1 lần, không có mode
    if (maxFrequency == 1) {
      return null;
    }

    // Tìm các điểm có tần suất cao nhất
    List<Double> modes = frequencyMap.entrySet().stream()
        .filter(entry -> entry.getValue() == maxFrequency)
        .map(Map.Entry::getKey)
        .sorted()
        .toList();

    // Nếu có nhiều hơn 1 mode, trả về null (không có mode duy nhất)
    if (modes.size() > 1) {
      return null;
    }

    return modes.get(0);
  }

  /**
   * Tính phân bố điểm theo khoảng: 0-2, 2-4, 4-6, 6-8, 8-10
   */
  private List<SessionExamDescriptiveStatisticResponse.ScoreDistributionItem> calculateScoreDistribution(
      List<Double> scores) {
    // Khởi tạo các khoảng điểm
    int[] ranges = {0, 2, 4, 6, 8, 10};
    Map<String, Integer> distributionMap = new HashMap<>();
    for (int i = 0; i < ranges.length - 1; i++) {
      String rangeKey = ranges[i] + "-" + ranges[i + 1];
      distributionMap.put(rangeKey, 0);
    }

    // Đếm số lượng điểm trong mỗi khoảng
    for (Double score : scores) {
      String rangeKey = getScoreRange(score);
      distributionMap.put(rangeKey, distributionMap.get(rangeKey) + 1);
    }

    // Chuyển đổi sang list
    List<SessionExamDescriptiveStatisticResponse.ScoreDistributionItem> distribution = new ArrayList<>();
    for (int i = 0; i < ranges.length - 1; i++) {
      String rangeKey = ranges[i] + "-" + ranges[i + 1];
      distribution.add(SessionExamDescriptiveStatisticResponse.ScoreDistributionItem.builder()
          .range(rangeKey)
          .count(distributionMap.get(rangeKey))
          .build());
    }

    return distribution;
  }

  /**
   * Xác định khoảng điểm cho một điểm số
   */
  private String getScoreRange(Double score) {
    if (score < 2) {
      return "0-2";
    } else if (score < 4) {
      return "2-4";
    } else if (score < 6) {
      return "4-6";
    } else if (score < 8) {
      return "6-8";
    } else {
      return "8-10";
    }
  }

  /**
   * Tạo phân bố điểm rỗng
   */
  private List<SessionExamDescriptiveStatisticResponse.ScoreDistributionItem> createEmptyScoreDistribution() {
    List<SessionExamDescriptiveStatisticResponse.ScoreDistributionItem> distribution = new ArrayList<>();
    String[] ranges = {"0-2", "2-4", "4-6", "6-8", "8-10"};
    for (String range : ranges) {
      distribution.add(SessionExamDescriptiveStatisticResponse.ScoreDistributionItem.builder()
          .range(range)
          .count(0)
          .build());
    }
    return distribution;
  }

  private SessionExamMonitoringResponse buildMonitoringLogData(SessionExam sessionExam) {
    String stateKey = RedisUtils.examSessionState(sessionExam.getSessionExamId());
    SessionExamStateDTO state = redisService.getExamState(stateKey);

    if (state == null) {
      log.warn("Session exam state {} not found", sessionExam.getSessionExamId());
      return SessionExamMonitoringResponse.builder().build();
    }

    // Get all students' status
    List<StudentStateInfo> students = this.getStudentsMonitoringInfo(
            sessionExam.getSessionExamId());

    // Get activity history từ Redis
    List<ActivityHistoryResponse> activityHistories = this.getActivityHistory(
            sessionExam.getSessionExamId());

    return SessionExamMonitoringResponse.builder()
            .sessionExamId(sessionExam.getSessionExamId())
            .title(state.getTitle())
            .description(state.getDescription())
            .phase(SessionExamPhase.ENDED)
            .countdownStartAt(state.getCountdownStartAt())
            .readyAt(state.getReadyAt())
            .examEndAt(state.getExamEndAt())
            .duration(state.getDuration())
            .totalStudents(state.getTotalStudents())
            .joinedCount(state.getJoinedCount())
            .downloadedCount(state.getDownloadedCount())
            .submittedCount(state.getSubmittedCount())
            .violationCount(state.getViolationCount())
            .activeCount(0)
            .students(students)
            .activityHistories(activityHistories)
            .serverTime(LocalDateTime.now())
            .build();
  }

  private List<ExamQuestionDTO> getAllQuestions(Long sessionExamId, QuestionOrderMode orderMode) {
    if (sessionExamId == null) {
      return null;
    }
    List<ExamQuestionSnapshot> examQuestionSnapshotList = examQuestionSnapshotRepository.findAllBySessionExamId(
        sessionExamId, SearchUtils.getSortQuestion(orderMode));
    if (examQuestionSnapshotList.isEmpty()) {
      throw new AppException(MessageConst.NOT_FOUND,
          messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
    }
    if (QuestionOrderMode.RANDOM.equals(orderMode)) {
      Collections.shuffle(examQuestionSnapshotList);
    }
    return examQuestionSnapshotList.stream()
        .map(ExamQuestionDTO::fromEntity)
        .toList();
  }

  @Transactional
  public void processFlexExamStarted(LocalDateTime now) {
    log.debug("Starting processFlexExamStarted job");

    // Xử lý các bài thi đã bắt đầu làm
    List<StudentSessionExam> activeFlexExams = studentSessionExamRepository
            .findActiveFlexExams(ExamSubmissionStatus.NOT_SUBMITTED);


    for (StudentSessionExam studentSessionExam : activeFlexExams) {
      try {
        SessionExam sessionExam = studentSessionExam.getSessionExam();

        if (sessionExam == null) {
          log.warn("SessionExam not found for sessionExamId: {}",
                  studentSessionExam.getSessionExamId());
          continue;
        }
        LocalDateTime examEndTime = studentSessionExam.getExamStartTime().plusMinutes(sessionExam.getDuration());
        boolean isExpired = now.isAfter(examEndTime) || now.isEqual(examEndTime);

        // Lấy dữ liệu từ Redis
        String examContentKey = RedisUtils.studentExamContent(
                studentSessionExam.getSessionExamId(),
                studentSessionExam.getStudentId());
        // lưu định kỳ bài thi vào db
        ExamQuestionsResponse examContent = redisService.get(examContentKey, ExamQuestionsResponse.class);
        if (examContent != null) {
          this.saveExamProgressToDb(studentSessionExam, examContent);
        }

          // Nếu đã hết thời gian, auto-submit
        if (isExpired) {
          log.info("Auto-submitting expired Flex exam for student {} in session exam {} - Time expired at {}",
                  studentSessionExam.getStudentId(),
                  studentSessionExam.getSessionExamId(),
                  examEndTime);

          autoSubmitFlexExam(studentSessionExam, sessionExam, examContent, examEndTime);
        }
      } catch (Exception e) {
        log.error("Error processing Flex exam for student {} in session exam {}: {}",
                studentSessionExam.getStudentId(),
                studentSessionExam.getSessionExamId(),
                e.getMessage(), e);
      }
    }
  }
  /**
   * Xử lý các bài thi FLEX đã qua endDate nhưng sinh viên chưa làm bài
   * Gán điểm 0
   */
  @Transactional
  public void processExpiredNotStartedFlexExams(LocalDateTime now) {

      List<StudentSessionExam> studentExams = studentSessionExamRepository
              .findNotStartedExpiredFlexExamsWithSessionExam(now, ExamSubmissionStatus.NOT_STARTED);

      if (studentExams.isEmpty()) {
        return;
      }

      for (StudentSessionExam studentExam : studentExams) {
          SessionExam sessionExam = studentExam.getSessionExam();
          if (sessionExam == null) {
            log.warn("SessionExam not found for student {} in session exam {}",
                    studentExam.getStudentId(), studentExam.getSessionExamId());
            continue;
          }
          List<ExamQuestionSnapshot> questionSnapshots = examQuestionSnapshotRepository
                  .findAllBySessionExamId(studentExam.getSessionExamId());
          updateStudentSessionExamFlexExam(
                  studentExam, 0.0, questionSnapshots, new HashMap<>(), null, null);
          log.info("Successfully auto-submitted not-started Flex exam for student {} in session exam {} with score 0",
                  studentExam.getStudentId(), studentExam.getSessionExamId());
      }
  }

  /**
   * Lưu bài làm từ Redis vào DB (submissionResult) - chỉ lưu bài làm, chưa submit
   */
  public void saveExamProgressToDb(StudentSessionExam studentSessionExam, ExamQuestionsResponse examContent) {
    if (examContent == null) {
      log.warn("No exam content found in Redis when trying to save progress for student {} in session exam {}",
              studentSessionExam.getStudentId(), studentSessionExam.getSessionExamId());
      return;
    }
    try {
      String submissionResultJson = objectMapper.writeValueAsString(examContent);

      studentSessionExam.setSubmissionResult(submissionResultJson);
      studentSessionExamRepository.saveAndFlush(studentSessionExam);

      log.debug("Saved exam progress (raw Redis payload) to DB for student {} in session exam {}",
              studentSessionExam.getStudentId(), studentSessionExam.getSessionExamId());
    } catch (JsonProcessingException e) {
      log.error("Error converting exam progress to JSON for student {} in session exam {}: {}",
              studentSessionExam.getStudentId(), studentSessionExam.getSessionExamId(), e.getMessage());
    }
  }

  /**
   * Auto-submit bài thi bắt đầu nhưng chưa submit khi hết thời gian
   */
  protected void autoSubmitFlexExam(StudentSessionExam studentSessionExam,
                                    SessionExam sessionExam,
                                    ExamQuestionsResponse examContent,
                                    LocalDateTime examEndTime) {

    List<ExamQuestionSnapshot> questionSnapshots = examQuestionSnapshotRepository
            .findAllBySessionExamId(studentSessionExam.getSessionExamId());

    double totalScore = calculateScore(questionSnapshots, examContent);

    Map<Long, List<Long>> selectedAnswersMap = extractSelectedAnswerMap(examContent);
    if (selectedAnswersMap.isEmpty()) {
      ExamQuestionsResponse fallback = readSubmissionResult(studentSessionExam);
      selectedAnswersMap = extractSelectedAnswerMap(fallback);
    }

    LocalDateTime examStartTime = studentSessionExam.getExamStartTime();
    updateStudentSessionExamFlexExam(studentSessionExam, totalScore, questionSnapshots, selectedAnswersMap, examEndTime, examStartTime);

    deleteRedisData(studentSessionExam.getSessionExamId(), studentSessionExam.getStudentId());

    log.info("Successfully auto-submitted Flex exam for student {} in session exam {} with score {}",
            studentSessionExam.getStudentId(), studentSessionExam.getSessionExamId(), totalScore);
  }

  /**
   * Tính điểm từ questionSnapshots và examContent
   * Điểm = (Số câu đúng / Tổng số câu) * 10
   */
  private double calculateScore(List<ExamQuestionSnapshot> questionSnapshots, ExamQuestionsResponse examContent) {
    if (questionSnapshots == null || questionSnapshots.isEmpty()) {
      return 0.0;
    }

    int totalQuestions = questionSnapshots.size();
    int correctCount = 0;

    for (ExamQuestionSnapshot questionSnapshot : questionSnapshots) {
      // Tìm câu trả lời của sinh viên từ ExamContent (nếu có)
      List<Long> selectedIds = null;
      if (examContent != null && examContent.getQuestions() != null) {
        ExamQuestionSnapshotResponse questionResponse = examContent.getQuestions().stream()
                .filter(q -> q.getId().equals(questionSnapshot.getId()))
                .findFirst()
                .orElse(null);

        if (questionResponse != null && questionResponse.getSelectedAnswerIds() != null) {
          selectedIds = questionResponse.getSelectedAnswerIds();
        }
      }
      
      if (selectedIds == null) {
        selectedIds = Collections.emptyList();
      }

      // Lấy danh sách đáp án đúng
      List<ExamQuestionAnswerSnapshot> correctAnswers = questionSnapshot.getExamQuestionAnswers().stream()
              .filter(ExamQuestionAnswerSnapshot::getIsCorrect)
              .toList();

      boolean isCorrect = false;
      if (!selectedIds.isEmpty()) {
        List<Long> correctIds = correctAnswers.stream()
                .map(ExamQuestionAnswerSnapshot::getId)
                .toList();

        // Logic kiểm tra đáp án đúng (giống Thi LIVE)
        if (questionSnapshot.getQuestionType() == QuestionType.MULTI_CHOICE) {
          // Single choice logic (chọn đúng 1 đáp án và đáp án đó đúng)
          isCorrect = selectedIds.size() == 1 && new HashSet<>(correctIds).containsAll(selectedIds)
              && correctIds.size() == 1;
        } else {
          // Multiple choice logic (chọn đúng tất cả đáp án đúng)
          isCorrect =
              selectedIds.size() == correctIds.size() && new HashSet<>(correctIds).containsAll(selectedIds);
        }
      }

      if (isCorrect) {
        correctCount++;
      }
    }

    // Tính điểm: (Số câu đúng / Tổng số câu) * 10
    return (double) correctCount / totalQuestions * 10.0;
  }

  /**
   * Cập nhật StudentSessionExam sau khi auto-submit
   */
  private void updateStudentSessionExamFlexExam(StudentSessionExam studentSessionExam,
                                                       double totalScore,
                                                       List<ExamQuestionSnapshot> questionSnapshots,
                                                       Map<Long, List<Long>> selectedAnswersMap,
                                                       LocalDateTime submissionTime,
                                                       LocalDateTime examStartTime) {
    SessionExamSubmissionDTO sessionExamSubmissionDTO = null;
    if (questionSnapshots != null && !questionSnapshots.isEmpty()) {
      sessionExamSubmissionDTO = buildSubmissionResultPayload(questionSnapshots, selectedAnswersMap);
    }
    
    // Cập nhật StudentSessionExam
    if(studentSessionExam.getSubmissionStatus()== ExamSubmissionStatus.NOT_STARTED) {
      studentSessionExam.setSubmissionStatus(ExamSubmissionStatus.NOT_SUBMITTED);
    }
    else {
      studentSessionExam.setSubmissionStatus(ExamSubmissionStatus.SUBMITTED);
    }
    studentSessionExam.setScore(totalScore);
    studentSessionExam.setSubmissionTime(submissionTime);
    if (examStartTime != null) {
      studentSessionExam.setExamStartTime(examStartTime);
    }
    if (sessionExamSubmissionDTO != null) {
      String submissionResultJson = JsonUtils.convertToJson(sessionExamSubmissionDTO);
      studentSessionExam.setSubmissionResult(submissionResultJson);
    } else {
      studentSessionExam.setSubmissionResult(null);
    }
    
    studentSessionExamRepository.saveAndFlush(studentSessionExam);
  }

  /**
   * Xóa dữ liệu Redis cho sinh viên này
   */
  private void deleteRedisData(Long sessionExamId, Long studentId) {
    try {
      String examContentKey = RedisUtils.studentExamContent(sessionExamId, studentId);
      String statusKey = RedisUtils.studentStatus(sessionExamId, studentId);
      String questionOrderKey = RedisUtils.studentQuestionOrder(sessionExamId, studentId);

      // Xóa tất cả các keys liên quan
      redisService.delete(examContentKey);
      redisService.delete(statusKey);
      redisService.delete(questionOrderKey);

      log.debug("Deleted Redis data for student {} in session exam {}", studentId, sessionExamId);
    } catch (Exception e) {
      log.error("Error deleting Redis data for student {} in session exam {}: {}",
              studentId, sessionExamId, e.getMessage());
    }
  }


  private ExamQuestionsResponse convertSnapshotsToExamContent(SessionExam sessionExam,
                                                              StudentSessionExam studentSessionExam,
                                                              List<ExamQuestionSnapshot> questionSnapshots) {
    List<ExamQuestionSnapshotResponse> questions = questionSnapshots == null ? new ArrayList<>() :
            questionSnapshots.stream()
                    .map(ExamQuestionSnapshotResponse::fromEntity)
                    .collect(Collectors.toList());
    for (ExamQuestionSnapshotResponse question : questions) {
      if (question.getSelectedAnswerIds() == null) {
        question.setSelectedAnswerIds(new ArrayList<>());
      }
    }
    return ExamQuestionsResponse.builder()
            .sessionExamId(sessionExam.getSessionExamId())
            .examId(sessionExam.getExamId())
            .title(sessionExam.getTitle())
            .duration(sessionExam.getDuration())
            .examStartTime(studentSessionExam.getExamStartTime())
            .endDate(sessionExam.getEndDate())
            .canSubmit(false)
            .submissionStatus(ExamSubmissionStatus.SUBMITTED)
            .questions(questions)
            .build();
  }


}


