package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.approval.*;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.approval.ApproveRejectRequest;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestDetailResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestItemResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestSearchResponse;
import com.vn.backend.dto.response.answer.AnswerResponse;
import com.vn.backend.dto.response.classroom.ClassroomDetailResponse;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.question.QuestionDetailResponse;
import com.vn.backend.dto.response.topic.TopicDetailResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.entities.*;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.RequestType;
import com.vn.backend.enums.Role;
import java.util.Comparator;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ApprovalRequestServiceImpl extends BaseService implements ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestItemsRepository approvalRequestItemsRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AuthService authService;
    private final ClassroomRepository classroomRepository;
    public ApprovalRequestServiceImpl(
            MessageUtils messageUtils,
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalRequestItemsRepository approvalRequestItemsRepository,
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            AuthService authService,
            ClassroomRepository classroomRepository) {
        super(messageUtils);
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalRequestItemsRepository = approvalRequestItemsRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.authService = authService;
        this.classroomRepository = classroomRepository;
    }


    @Override
    public void createRequest(RequestType requestType, String requestDescription, Long requesterId, List<Long> entityIds) {
        log.info("Start service to create request");

        this.validateCreateRequest(requestType, requesterId, entityIds);

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(requestType)
                .description(requestDescription)
                .requesterId(requesterId)
                .build();
        approvalRequestRepository.save(approvalRequest);

        List<ApprovalRequestItems> approvalRequestItems = new ArrayList<>();
        for (Long id : entityIds){
            ApprovalRequestItems item = ApprovalRequestItems.builder()
                    .requestId(approvalRequest.getId())
                    .entityId(id)
                    .entityType(AppConst.REQUEST_TYPE_ENTITY_TYPE_MAP.get(approvalRequest.getRequestType()))
                    .build();
            approvalRequestItems.add(item);
        }
        approvalRequestItemsRepository.saveAll(approvalRequestItems);

        log.info("End service create request");
    }

    private void validateCreateRequest(RequestType requestType, Long requesterId, List<Long> entityIds) {
        if (requestType == null) {
            log.warn("request type null");
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "requestType", HttpStatus.BAD_REQUEST);
        }

        if (requesterId == null) {
            log.warn("requester id null");
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "requesterId", HttpStatus.BAD_REQUEST);
        }
        if (entityIds == null || entityIds.isEmpty()) {
            log.warn("entity ids null");
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "entityIds", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ApprovalRequestDetailResponse getApprovalRequestDetail(Long id) {
        log.info("Start service to get approval request detail");

        User user = authService.getCurrentUser();
        Long requesterId = Role.TEACHER.equals(user.getRole()) ? user.getId() : null;
        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdWithDetails(id, requesterId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
        ApprovalRequestDetailResponse response = this.mapToApprovalRequestDetailResponse(approvalRequest);

        log.info("End service get approval request detail");
        return response;
    }

    @Override
    public ResponseListData<ApprovalRequestSearchResponse> searchApprovalRequest(BaseFilterSearchRequest<ApprovalRequestSearchRequest> request) {
        log.info("Start service to search approval request");

        User user = authService.getCurrentUser();
        ApprovalRequestSearchRequestDTO requestDTO = request.getFilters().toDTO();
        if (Role.TEACHER.equals(user.getRole())) {
            requestDTO.setUserId(user.getId());
        }
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<ApprovalRequest> queryDTOS = approvalRequestRepository.searchApprovalRequest(
                requestDTO, pageable
        );
        List<ApprovalRequestSearchResponse> response = queryDTOS.stream()
                .map(ApprovalRequestSearchResponse::fromEntity)
                .toList();
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());

        log.info("End service search approval request");
        return new ResponseListData<ApprovalRequestSearchResponse>(response, pagingMeta);
    }

    private void acceptCreateClassroomRequest(ApprovalRequest request) {
        List<ApprovalRequestItems> items = approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(request.getId());
        if (items.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND, messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND);
        }
        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatus(
                        items.stream().findFirst().get().getEntityId(),
                        ClassroomStatus.ACTIVE)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
        classroom.setIsActive(true);
        classroomRepository.save(classroom);
    }

    private ApprovalRequestDetailResponse mapToApprovalRequestDetailResponse(ApprovalRequest approvalRequest) {
        ApprovalRequestDetailResponse response = ApprovalRequestDetailResponse.fromEntity(approvalRequest);

        List<ApprovalRequestItems> items = approvalRequest.getItems() != null
                ? approvalRequest.getItems().stream()
                .filter(item -> !item.getIsDeleted())
                .toList()
                : approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(approvalRequest.getId());

        switch (approvalRequest.getRequestType()) {
            case CLASS_CREATE -> {
                Classroom classroom = classroomRepository.findById(approvalRequest.getItems().get(0).getEntityId())
                        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
                response.setRequestItems(
                        List.of(ApprovalRequestItemResponse.fromEntity(approvalRequest.getItems().get(0)))
                );
                response.getRequestItems().get(0).setClassroom(ClassroomDetailResponse.fromEntity(classroom));
            }
            case TOPIC_CREATE -> {
                List<ApprovalRequestItemResponse> itemResponses = new ArrayList<>();

                // Lấy subjectId từ topic đầu tiên trong request items
                Long subjectId = null;
                if (!items.isEmpty()) {
                    Topic firstTopic = topicRepository.findByTopicIdAndIsDeletedFalse(items.get(0).getEntityId());
                    if (firstTopic != null) {
                        subjectId = firstTopic.getSubjectId();
                    }
                }

                // Map request items
                for (ApprovalRequestItems item : items) {
                    Topic topic = topicRepository.findByTopicIdAndIsDeletedFalse(item.getEntityId());
                    if (topic == null) {
                        continue;
                    }

                    ApprovalRequestItemResponse itemResponse = ApprovalRequestItemResponse.fromEntity(item);
                    itemResponse.setTopicResponse(TopicResponse.builder()
                            .topicId(topic.getTopicId())
                            .topicName(topic.getTopicName())
                            .subjectId(topic.getSubjectId())
                            .isDeleted(topic.getIsDeleted())
                            .createdAt(topic.getCreatedAt())
                            .updatedAt(topic.getUpdatedAt())
                            .build());
                    itemResponses.add(itemResponse);
                }

                response.setRequestItems(itemResponses);

                // Lấy các topics đang active của môn học
                List<TopicDetailResponse> currentTopics = new ArrayList<>();
                if (subjectId != null) {
                    List<Topic> activeTopics = topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(subjectId);
                    for (Topic topic : activeTopics) {
                        TopicDetailResponse.PrerequisiteTopicResponse prerequisiteResponse = null;
                        if (topic.getPrerequisiteTopicId() != null && topic.getPrerequisiteTopic() != null) {
                            Topic prereqTopic = topic.getPrerequisiteTopic();
                            prerequisiteResponse = TopicDetailResponse.PrerequisiteTopicResponse.builder()
                                    .topicId(prereqTopic.getTopicId())
                                    .topicName(prereqTopic.getTopicName())
                                    .build();
                        }

                        currentTopics.add(TopicDetailResponse.builder()
                                .topicId(topic.getTopicId())
                                .topicName(topic.getTopicName())
                                .subjectId(topic.getSubjectId())
                                .subjectName(topic.getSubject() != null ? topic.getSubject().getSubjectName() : null)
                                .prerequisiteTopic(prerequisiteResponse)
                                .build());
                    }
                }

                response.setCurrentTopics(currentTopics);
            }
            case QUESTION_REVIEW_CREATE -> {
                List<ApprovalRequestItemResponse> itemResponses = new ArrayList<>();

                for (ApprovalRequestItems item : items) {
                    Question question = questionRepository.findById(item.getEntityId())
                            .orElse(null);

                    if (question == null || question.getIsDeleted()) {
                        continue;
                    }

                    ApprovalRequestItemResponse itemResponse = ApprovalRequestItemResponse.fromEntity(item);

                    // Map Question to QuestionDetailResponse
                    QuestionDetailResponse questionDetail = mapQuestionToDetailResponse(question);
                    itemResponse.setQuestion(questionDetail);

                    itemResponses.add(itemResponse);
                }

                response.setRequestItems(itemResponses);
            }
            default -> {
                return response;
            }
        }

        return response;
    }

    private QuestionDetailResponse mapQuestionToDetailResponse(Question question) {
        QuestionDetailResponse detail = new QuestionDetailResponse();
        detail.setId(question.getQuestionId());
        detail.setContent(question.getContent());
        detail.setImageUrl(question.getImageUrl());
        detail.setType(question.getType() != null ? question.getType().toString() : null);
        detail.setDifficultyLevel(question.getDifficultyLevel());
        detail.setCreatedBy(question.getCreatedBy());

        // Map topic
        if (question.getTopic() != null) {
            Topic topic = question.getTopic();
            detail.setTopic(TopicResponse.builder()
                    .topicId(topic.getTopicId())
                    .topicName(topic.getTopicName())
                    .subjectId(topic.getSubjectId())
                    .isDeleted(topic.getIsDeleted())
                    .createdAt(topic.getCreatedAt())
                    .updatedAt(topic.getUpdatedAt())
                    .build());
        }

        // Map answers
        List<Answer> answers = answerRepository.findByQuestionIdOrderByDisplayOrder(question.getQuestionId());
        List<AnswerResponse> answerResponses = answers.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .map(a -> {
                    AnswerResponse ar = new AnswerResponse();
                    ar.setId(a.getAnswerId());
                    ar.setContent(a.getContent());
                    ar.setIsCorrect(a.getIsCorrect());
                    ar.setDisplayOrder(a.getDisplayOrder());
                    return ar;
                })
                .sorted(Comparator.comparing(AnswerResponse::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        detail.setAnswers(answerResponses);

        return detail;
    }

    @Override
    @Transactional
    public void approveRequest(Long requestId) {
        log.info("Start service to approve request: {}", requestId);

        User currentUser = authService.getCurrentUser();

        // Validate chỉ ADMIN mới được approve
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.FORBIDDEN);
        }

        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        // Validate request status phải là PENDING
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    "Request is not in PENDING status", HttpStatus.BAD_REQUEST);
        }

        // Update request status
        approvalRequest.setStatus(ApprovalStatus.APPROVED);
        approvalRequest.setReviewerId(currentUser.getId());
        approvalRequestRepository.save(approvalRequest);

        // Process based on request type
        switch (approvalRequest.getRequestType()) {
            case TOPIC_CREATE -> approveTopicRequest(approvalRequest);
            case QUESTION_REVIEW_CREATE -> approveQuestionRequest(approvalRequest);
            case CLASS_CREATE -> acceptCreateClassroomRequest(approvalRequest);
            default -> {
                log.warn("Unknown request type: {}", approvalRequest.getRequestType());
            }
        }

        log.info("End service approve request: {}", requestId);
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, ApproveRejectRequest request) {
        log.info("Start service to reject request: {}", requestId);

        User currentUser = authService.getCurrentUser();

        // Validate chỉ ADMIN mới được reject
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.FORBIDDEN);
        }

        ApprovalRequest approvalRequest = approvalRequestRepository.findByIdAndIsDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));

        // Validate request status phải là PENDING
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    "Request is not in PENDING status", HttpStatus.BAD_REQUEST);
        }

        // Validate reject reason
        if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    "Reject reason is required", HttpStatus.BAD_REQUEST);
        }

        // Update request status
        approvalRequest.setStatus(ApprovalStatus.REJECTED);
        approvalRequest.setReviewerId(currentUser.getId());
        approvalRequest.setRejectReason(request.getRejectReason());
        approvalRequestRepository.save(approvalRequest);

        log.info("End service reject request: {}", requestId);
    }

    private void approveTopicRequest(ApprovalRequest approvalRequest) {
        log.info("Approving topic request: {}", approvalRequest.getId());

        List<ApprovalRequestItems> items = approvalRequestItemsRepository
                .findByRequestIdAndIsDeletedFalse(approvalRequest.getId());

        items.sort(Comparator.comparing(ApprovalRequestItems::getCreatedAt));

        // Lấy tất cả entityIds
        List<Long> topicIds = items.stream()
                .map(ApprovalRequestItems::getEntityId)
                .toList();

        List<Topic> approvedTopics = topicRepository.findByTopicIdInAndIsDeletedFalse(topicIds);
        if (approvedTopics.isEmpty()) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    "No topics found for this request", HttpStatus.BAD_REQUEST);
        }
        List<Long> newlyActivatedTopicIds = new ArrayList<>(); 
        for (int i = 0; i < approvedTopics.size(); i++) {
            Topic topic = approvedTopics.get(i);
            topic.setIsActive(true);

            if (i > 0) {
                topic.setPrerequisiteTopicId(approvedTopics.get(i - 1).getTopicId());
            }

            newlyActivatedTopicIds.add(topic.getTopicId());
            log.info("Activated topic {} with prerequisite {}", topic.getTopicId(), topic.getPrerequisiteTopicId());
        }
    
        topicRepository.saveAll(approvedTopics);

        Long subjectId = approvedTopics.get(0).getSubjectId();

        List<Topic> activeTopics =
                topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(subjectId);
        List<Topic> topicsToDeactivate = new ArrayList<>();
        for (Topic oldTopic : activeTopics) {
            if (!newlyActivatedTopicIds.contains(oldTopic.getTopicId())) {
                oldTopic.setIsActive(false);
                topicsToDeactivate.add(oldTopic);
            }
        }
        if (!topicsToDeactivate.isEmpty()) {
            topicRepository.saveAll(topicsToDeactivate);
        }
        log.info("Approved {} topics", approvedTopics.size());
    }

    private void approveQuestionRequest(ApprovalRequest approvalRequest) {
        log.info("Approving question request: {}", approvalRequest.getId());

        // Lấy danh sách ApprovalRequestItems
        List<ApprovalRequestItems> items = approvalRequestItemsRepository
                .findByRequestIdAndIsDeletedFalse(approvalRequest.getId());

        for (ApprovalRequestItems item : items) {
            Question originalQuestion = questionRepository.findById(item.getEntityId())
                    .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                            "Question not found: " + item.getEntityId(), HttpStatus.NOT_FOUND));

            if (Boolean.TRUE.equals(originalQuestion.getIsDeleted())) {
                throw new AppException(AppConst.MessageConst.NOT_FOUND,
                        "Question is deleted: " + item.getEntityId(), HttpStatus.BAD_REQUEST);
            }

            // Validate topic tồn tại và active
            Topic topic = topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(originalQuestion.getTopicId())
                    .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                            "Topic not found or not active: " + originalQuestion.getTopicId(), HttpStatus.BAD_REQUEST));

            // Clone Question với isReviewQuestion = true (câu hỏi ôn tập)
            Question reviewQuestion = Question.builder()
                    .content(originalQuestion.getContent())
                    .imageUrl(originalQuestion.getImageUrl())
                    .type(originalQuestion.getType())
                    .difficultyLevel(originalQuestion.getDifficultyLevel())
                    .topicId(originalQuestion.getTopicId())
                    .createdBy(originalQuestion.getCreatedBy())
                    .isDeleted(false)
                    .isReviewQuestion(true) // Đánh dấu là câu hỏi ôn tập
                    .isAddedToReview(false) // Reset flag này
                    .build();

            Question savedReviewQuestion = questionRepository.save(reviewQuestion);

            // Clone tất cả Answer từ Question gốc
            List<Answer> originalAnswers = answerRepository.findByQuestionIdOrderByDisplayOrder(originalQuestion.getQuestionId());

            for (Answer originalAnswer : originalAnswers) {
                if (Boolean.TRUE.equals(originalAnswer.getIsDeleted())) {
                    continue; // Bỏ qua answer đã bị xóa
                }

                Answer reviewAnswer = Answer.builder()
                        .content(originalAnswer.getContent())
                        .questionId(savedReviewQuestion.getQuestionId())
                        .isCorrect(originalAnswer.getIsCorrect())
                        .displayOrder(originalAnswer.getDisplayOrder())
                        .isDeleted(false)
                        .build();

                answerRepository.save(reviewAnswer);
            }

            // Đánh dấu question gốc đã được thêm vào review
            originalQuestion.setIsAddedToReview(true);
            questionRepository.save(originalQuestion);

            log.info("Created review Question {} from original Question {}", savedReviewQuestion.getQuestionId(),
                    originalQuestion.getQuestionId());
        }

        log.info("Approved {} questions", items.size());
    }
}

