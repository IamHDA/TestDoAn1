package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.topic.CreateApprovalTopicRequest;
import com.vn.backend.dto.request.topic.CreateTopicRequest;
import com.vn.backend.dto.request.topic.TopicFilterRequest;
import com.vn.backend.dto.request.topic.UpdateTopicRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.topic.TopicDetailResponse;
import com.vn.backend.dto.response.topic.TopicResponse;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.EntityType;
import com.vn.backend.enums.RequestType;
import com.vn.backend.repositories.ApprovalRequestItemsRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.TopicService;
import com.vn.backend.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TopicServiceImpl extends BaseService implements TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final ApprovalRequestItemsRepository approvalRequestItemsRepository;
    private final AuthService authService;
    private final ApprovalRequestService approvalRequestService;


    public TopicServiceImpl(MessageUtils messageUtils, TopicRepository topicRepository,
                            SubjectRepository subjectRepository,
                            ApprovalRequestItemsRepository approvalRequestItemsRepository,
                            AuthService authService,
                            ApprovalRequestService approvalRequestService
    ) {
        super(messageUtils);
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.approvalRequestItemsRepository = approvalRequestItemsRepository;
        this.authService = authService;
        this.approvalRequestService = approvalRequestService;
    }

    @Override
    @Transactional
    public void approvalRequestTopic(CreateApprovalTopicRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // Validate chỉ TEACHER mới được tạo đề xuất
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.BAD_REQUEST);
        }
        
        // Validate subject tồn tại và active
        validateSubjectExists(request.getSubjectId());
        
        // Validate topicRequests không null và không rỗng
        if (request.getTopicRequests() == null || request.getTopicRequests().isEmpty()) {
            throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                    "Topic requests cannot be empty", HttpStatus.BAD_REQUEST);
        }
        
        // Kiểm tra nếu subject này đang tồn tại một requestApproval PENDING thì trả ra lỗi
        // Lấy các topicIds từ các ApprovalRequest PENDING của user hiện tại
        List<Long> pendingTopicIds = approvalRequestItemsRepository.findEntityIdsByPendingRequest(
                RequestType.TOPIC_CREATE,
                ApprovalStatus.PENDING,
                EntityType.TOPIC
        );
        
        // Kiểm tra xem có topic nào trong pending request thuộc subject này không
        if (!pendingTopicIds.isEmpty()) {
            List<Topic> pendingTopics = topicRepository.findAllById(pendingTopicIds);
            boolean hasTopicInSameSubject = pendingTopics.stream()
                    .filter(topic -> !topic.getIsDeleted()) // Chỉ kiểm tra topic chưa bị xóa
                    .anyMatch(topic -> topic.getSubjectId().equals(request.getSubjectId()));
            
            if (hasTopicInSameSubject) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        "Đang tồn tại một yêu cầu tạo chủ đề cho môn học này đang chờ duyệt. Vui lòng chờ admin xét duyệt hoặc hủy yêu cầu cũ trước khi tạo yêu cầu mới.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        List<Topic> topics = saveAllTopic(request.getTopicRequests(), request.getSubjectId(), currentUser);
        
        // Lấy danh sách topicId để tạo ApprovalRequestItems
        List<Long> topicIds = topics.stream()
                .map(Topic::getTopicId)
                .collect(Collectors.toList());

        approvalRequestService.createRequest(
                request.getRequestType(),
                request.getRequestDescription(),
                currentUser.getId(),
                topicIds
        );
    }

    @Transactional
    public List<Topic> saveAllTopic(List<CreateTopicRequest> listTopic, Long subjectId, User currentUser) {
        List<Topic> topics = new ArrayList<>();

        for (CreateTopicRequest req : listTopic) {
            if (req.getTopicName() == null || req.getTopicName().trim().isEmpty()) {
                throw new AppException(AppConst.MessageConst.INVALID_LOGIC_QUESTION,
                        "Topic name cannot be empty", HttpStatus.BAD_REQUEST);
            }
            
            Topic topic;

            if (req.getTopicId() == null) {
                topic = Topic.builder()
                        .topicName(req.getTopicName().trim())
                        .subjectId(subjectId)
                        .createdBy(currentUser.getId())
                        .isDeleted(false)
                        .isActive(false)
                        .prerequisiteTopicId(req.getPrerequisiteTopicId())
                        .build();
            } else {
                topic = topicRepository.findByTopicIdAndIsDeleted(req.getTopicId(), Boolean.FALSE)
                        .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                                "Topic not found with id: " + req.getTopicId(), HttpStatus.NOT_FOUND));
            }
            topics.add(topic);
        }
        return topicRepository.saveAllAndFlush(topics);
    }


    @Override
    @Transactional
    public void updateTopic(Long topicId, UpdateTopicRequest request) {
        log.info("Updating topic: {}", topicId);
        Topic topic = topicRepository.findByTopicIdAndIsDeleted(topicId, false)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        User currentUser = authService.getCurrentUser();
        if(currentUser.getRole() != Role.ADMIN) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.BAD_REQUEST);
        }
        // Update topic name if provided
        if(request.getTopicName() != null) {
            topic.setTopicName(request.getTopicName());
        }
        // Update prerequisite topic if provided - mỗi topic chỉ có tối đa 1 topic tiên quyết
        if (request.getPrerequisiteTopicId() != null) {
            // Validate prerequisite topic exists
            if (request.getPrerequisiteTopicId().equals(topicId)) {
                throw new AppException(AppConst.MessageConst.NOT_FOUND,
                        "Topic cannot be its own prerequisite", HttpStatus.BAD_REQUEST);
            }
            topicRepository.findByTopicIdAndIsDeleted(request.getPrerequisiteTopicId(), false)
                    .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
            topic.setPrerequisiteTopicId(request.getPrerequisiteTopicId());
        }

        topicRepository.save(topic);
        log.info("Updated topic: {}", topicId);
    }


    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        log.info("Deleting topic: {}", topicId);
        Topic topic = topicRepository.findByTopicIdAndIsDeleted(topicId, false)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        User currentUser = authService.getCurrentUser();
        if(currentUser.getRole() != Role.ADMIN) {
            throw new AppException(AppConst.MessageConst.UNAUTHORIZED,
                    messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED), HttpStatus.BAD_REQUEST);
        }
        topic.setIsDeleted(true);
        topicRepository.save(topic);
        log.info("Soft-deleted topic: {}", topicId);
    }

    @Override
    public ResponseListData<TopicResponse> searchTopics(BaseFilterSearchRequest<TopicFilterRequest> request) {
        TopicFilterRequest filters = request.getFilters();
        if (filters == null) {
            filters = new TopicFilterRequest();
        }
        
        if (filters.getSubjectId() != null) {
            validateSubjectExists(filters.getSubjectId());
        }

        PagingMeta paging = request.getPagination().getPagingMeta();
        Pageable pageable = paging.toPageable();
        Page<Topic> page = topicRepository.findTopicsBySubjectIdWithSearch(
                filters.getSubjectId(), filters.getKeyword(), pageable);
        paging.setTotalRows(page.getTotalElements());
        paging.setTotalPages(page.getTotalPages());

        return new ResponseListData<>(
                page.map(t -> TopicResponse.builder()
                        .topicId(t.getTopicId())
                        .topicName(t.getTopicName())
                        .subjectId(t.getSubjectId())
                        .isDeleted(t.getIsDeleted())
                        .createdAt(t.getCreatedAt())
                        .updatedAt(t.getUpdatedAt())
                        .build()).getContent(),
                paging
        );
    }


    private void validateSubjectExists(Long subjectId) {
        if (!subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(subjectId)) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
        }
    }
}



