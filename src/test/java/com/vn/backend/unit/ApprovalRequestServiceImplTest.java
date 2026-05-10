package com.vn.backend.unit;


import org.junit.jupiter.api.DisplayName;

import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest;
import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequestDTO;
import com.vn.backend.dto.request.approval.ApproveRejectRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestDetailResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestSearchResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.ApprovalRequestServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalRequestServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TEACHER_ID = 4L;
    private static final Long REQUEST_ID = 10L;
    private static final Long CLASSROOM_ID = 20L;
    private static final Long TOPIC_ID = 30L;
    private static final Long QUESTION_ID = 40L;
    private static final Long SUBJECT_ID = 50L;

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ApprovalRequestItemsRepository approvalRequestItemsRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ClassroomRepository classroomRepository;

    private ApprovalRequestServiceImpl service;

    private final Map<Long, ApprovalRequest> approvalRequestStore = new HashMap<>();
    private final Map<Long, List<ApprovalRequestItems>> itemStore = new HashMap<>();
    private final Map<Long, Topic> topicStore = new HashMap<>();
    private final Map<Long, Question> questionStore = new HashMap<>();
    private final Map<Long, List<Answer>> answerStore = new HashMap<>();
    private final Map<Long, Classroom> classroomStore = new HashMap<>();

    private ApprovalRequest savedApprovalRequest;
    private List<Topic> savedTopics = new ArrayList<>();
    private List<Question> savedQuestions = new ArrayList<>();
    private Question savedQuestion;
    private Answer savedAnswer;
    private Classroom savedClassroom;

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new ApprovalRequestServiceImpl(
                messageUtils,
                approvalRequestRepository,
                approvalRequestItemsRepository,
                topicRepository,
                questionRepository,
                answerRepository,
                authService,
                classroomRepository
        );

        mockApprovalRequestRepository();
        mockApprovalRequestItemsRepository();
        mockTopicRepository();
        mockQuestionRepository();
        mockAnswerRepository();
        mockClassroomRepository();
    }

    private void mockApprovalRequestRepository() {
        when(approvalRequestRepository.findByIdWithDetails(anyLong(), any()))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    Long requesterId = invocation.getArgument(1);

                    ApprovalRequest request = approvalRequestStore.get(id);
                    if (request == null || Boolean.TRUE.equals(request.getIsDeleted())) {
                        return Optional.empty();
                    }

                    if (requesterId != null && !requesterId.equals(request.getRequesterId())) {
                        return Optional.empty();
                    }

                    return Optional.of(request);
                });

        when(approvalRequestRepository.findByIdAndIsDeletedFalse(anyLong()))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    ApprovalRequest request = approvalRequestStore.get(id);

                    if (request == null || Boolean.TRUE.equals(request.getIsDeleted())) {
                        return Optional.empty();
                    }

                    return Optional.of(request);
                });

        when(approvalRequestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> {
                    savedApprovalRequest = invocation.getArgument(0);
                    approvalRequestStore.put(savedApprovalRequest.getId(), savedApprovalRequest);
                    return savedApprovalRequest;
                });
    }

    private void mockApprovalRequestItemsRepository() {
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(anyLong()))
                .thenAnswer(invocation -> {
                    Long requestId = invocation.getArgument(0);

                    List<ApprovalRequestItems> items = itemStore.getOrDefault(requestId, List.of())
                            .stream()
                            .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                            .sorted(Comparator.comparing(
                                    ApprovalRequestItems::getCreatedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            ))
                            .toList();

                    return new ArrayList<>(items);
                });
    }

    private void mockTopicRepository() {
        when(topicRepository.findByTopicIdAndIsDeletedFalse(anyLong()))
                .thenAnswer(invocation -> {
                    Long topicId = invocation.getArgument(0);
                    Topic topic = topicStore.get(topicId);

                    if (topic == null || Boolean.TRUE.equals(topic.getIsDeleted())) {
                        return null;
                    }

                    return topic;
                });

        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(anyLong()))
                .thenAnswer(invocation -> {
                    Long subjectId = invocation.getArgument(0);

                    return new ArrayList<>(
                            topicStore.values()
                                    .stream()
                                    .filter(topic -> subjectId.equals(topic.getSubjectId()))
                                    .filter(topic -> Boolean.TRUE.equals(topic.getIsActive()))
                                    .filter(topic -> !Boolean.TRUE.equals(topic.getIsDeleted()))
                                    .toList()
                    );
                });

        when(topicRepository.findByTopicIdInAndIsDeletedFalse(any()))
                .thenAnswer(invocation -> {
                    List<Long> topicIds = invocation.getArgument(0);

                    return new ArrayList<>(
                            topicIds.stream()
                                    .map(topicStore::get)
                                    .filter(topic -> topic != null && !Boolean.TRUE.equals(topic.getIsDeleted()))
                                    .toList()
                    );
                });

        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(anyLong()))
                .thenAnswer(invocation -> {
                    Long topicId = invocation.getArgument(0);
                    Topic topic = topicStore.get(topicId);

                    if (topic == null
                            || Boolean.TRUE.equals(topic.getIsDeleted())
                            || !Boolean.TRUE.equals(topic.getIsActive())) {
                        return Optional.empty();
                    }

                    return Optional.of(topic);
                });

        when(topicRepository.saveAll(any())).thenAnswer(invocation -> {
            List<Topic> topics = invocation.getArgument(0);

            if (savedTopics.isEmpty()) {
                savedTopics = new ArrayList<>(topics);
            }

            topics.forEach(topic -> topicStore.put(topic.getTopicId(), topic));
            return topics;
        });
    }

    private void mockQuestionRepository() {
        when(questionRepository.findById(anyLong()))
                .thenAnswer(invocation -> {
                    Long questionId = invocation.getArgument(0);
                    Question question = questionStore.get(questionId);
                    return question == null ? Optional.empty() : Optional.of(question);
                });

        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);

            if (question.getQuestionId() == null) {
                question.setQuestionId(999L);
            }

            savedQuestion = question;
            savedQuestions.add(question);
            questionStore.put(question.getQuestionId(), question);

            return question;
        });
    }

    private void mockAnswerRepository() {
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(anyLong()))
                .thenAnswer(invocation -> {
                    Long questionId = invocation.getArgument(0);
                    return answerStore.getOrDefault(questionId, List.of());
                });

        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> {
            savedAnswer = invocation.getArgument(0);
            return savedAnswer;
        });
    }

    private void mockClassroomRepository() {
        when(classroomRepository.findById(anyLong()))
                .thenAnswer(invocation -> {
                    Long classroomId = invocation.getArgument(0);
                    Classroom classroom = classroomStore.get(classroomId);
                    return classroom == null ? Optional.empty() : Optional.of(classroom);
                });

        when(classroomRepository.findByClassroomIdAndClassroomStatus(anyLong(), any(ClassroomStatus.class)))
                .thenAnswer(invocation -> {
                    Long classroomId = invocation.getArgument(0);
                    ClassroomStatus status = invocation.getArgument(1);

                    Classroom classroom = classroomStore.get(classroomId);
                    if (classroom == null || classroom.getClassroomStatus() != status) {
                        return Optional.empty();
                    }

                    return Optional.of(classroom);
                });

        when(classroomRepository.save(any(Classroom.class))).thenAnswer(invocation -> {
            savedClassroom = invocation.getArgument(0);
            classroomStore.put(savedClassroom.getClassroomId(), savedClassroom);
            return savedClassroom;
        });
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .role(role)
                .fullName("User " + id)
                .build();
    }

    private void mockCurrentUser(Long id, Role role) {
        when(authService.getCurrentUser()).thenReturn(user(id, role));
    }

    private ApprovalRequest approvalRequest(
            Long id,
            RequestType requestType,
            ApprovalStatus status,
            Long requesterId
    ) {
        User requester = user(requesterId, Role.TEACHER);

        ApprovalRequest request = ApprovalRequest.builder()
                .id(id)
                .requestType(requestType)
                .description("Need approval")
                .requesterId(requesterId)
                .status(status)
                .isDeleted(false)
                .items(new ArrayList<>())
                .build();

        request.setRequester(requester);

        return request;
    }

    private ApprovalRequestItems item(Long requestId, Long entityId) {
        ApprovalRequestItems item = ApprovalRequestItems.builder()
                .requestId(requestId)
                .entityId(entityId)
                .isDeleted(false)
                .build();

        item.setCreatedAt(LocalDateTime.now().plusSeconds(entityId));

        return item;
    }

    private ApprovalRequest saveApprovalRequest(
            RequestType requestType,
            ApprovalStatus status,
            Long requesterId,
            List<Long> entityIds
    ) {
        ApprovalRequest request = approvalRequest(REQUEST_ID, requestType, status, requesterId);

        List<ApprovalRequestItems> items = entityIds.stream()
                .map(entityId -> item(REQUEST_ID, entityId))
                .toList();

        request.setItems(new ArrayList<>(items));
        approvalRequestStore.put(REQUEST_ID, request);
        itemStore.put(REQUEST_ID, new ArrayList<>(items));

        return request;
    }

    private Topic topic(Long topicId, Long subjectId, boolean active) {
        return Topic.builder()
                .topicId(topicId)
                .topicName("Topic " + topicId)
                .subjectId(subjectId)
                .isActive(active)
                .isDeleted(false)
                .build();
    }

    private Question question(Long questionId, Long topicId, boolean deleted) {
        Topic topic = topic(topicId, SUBJECT_ID, true);

        return Question.builder()
                .questionId(questionId)
                .content("Question content")
                .imageUrl("image.png")
                .type(QuestionType.SINGLE_CHOICE)
                .difficultyLevel(1)
                .topicId(topicId)
                .topic(topic)
                .createdBy(TEACHER_ID)
                .isDeleted(deleted)
                .isReviewQuestion(false)
                .isAddedToReview(false)
                .build();
    }

    private Answer answer(Long answerId, Long questionId, String content, boolean correct, Integer order, boolean deleted) {
        return Answer.builder()
                .answerId(answerId)
                .questionId(questionId)
                .content(content)
                .isCorrect(correct)
                .displayOrder(order)
                .isDeleted(deleted)
                .build();
    }

    private Classroom classroom(Long classroomId) {
        Subject subject = Subject.builder()
                .subjectId(SUBJECT_ID)
                .subjectName("Java")
                .build();

        User teacher = User.builder()
                .id(TEACHER_ID)
                .fullName("Teacher")
                .build();

        return Classroom.builder()
                .classroomId(classroomId)
                .classCode("ABC123")
                .className("SE Class")
                .teacherId(TEACHER_ID)
                .teacher(teacher)
                .subject(subject)
                .description("Description")
                .coverImageUrl("cover.png")
                .classroomStatus(ClassroomStatus.ACTIVE)
                .classCodeStatus(ClassCodeStatus.ACTIVE)
                .isActive(false)
                .schedules(new ArrayList<ClassSchedule>())
                .build();
    }

    private ApproveRejectRequest rejectRequest(String reason) {
        ApproveRejectRequest request = new ApproveRejectRequest();
        request.setRejectReason(reason);
        return request;
    }

    private BaseFilterSearchRequest<ApprovalRequestSearchRequest> searchRequest() {
        BaseFilterSearchRequest<ApprovalRequestSearchRequest> request = mock(BaseFilterSearchRequest.class);
        ApprovalRequestSearchRequest filters = mock(ApprovalRequestSearchRequest.class);
        ApprovalRequestSearchRequestDTO dto = mock(ApprovalRequestSearchRequestDTO.class);

        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");

        when(request.getFilters()).thenReturn(filters);
        when(filters.toDTO()).thenReturn(dto);
        when(request.getPagination()).thenReturn(pagination);

        return request;
    }

    @Nested
    class GetApprovalRequestDetailTests {

        @Test
        @DisplayName("HT_DY_01 - Đảm bảo Admin xem chi tiết yêu cầu tạo lớp thành công.")
        void getApprovalRequestDetail_Success_ClassCreate() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Classroom classroom = classroom(CLASSROOM_ID);
            classroomStore.put(CLASSROOM_ID, classroom);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            ApprovalRequestDetailResponse response = service.getApprovalRequestDetail(REQUEST_ID);

            assertNotNull(response);
            assertNotNull(response.getRequestItems());
            assertEquals(1, response.getRequestItems().size());
            assertNotNull(response.getRequestItems().get(0).getClassroom());
        }

        @Test
        @DisplayName("HT_DY_02 - Đảm bảo Admin xem chi tiết yêu cầu tạo chủ đề thành công.")
        void getApprovalRequestDetail_Success_TopicCreate() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic oldTopic = topic(1L, SUBJECT_ID, true);
            Topic newTopic = topic(TOPIC_ID, SUBJECT_ID, false);

            topicStore.put(1L, oldTopic);
            topicStore.put(TOPIC_ID, newTopic);

            saveApprovalRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(TOPIC_ID)
            );

            ApprovalRequestDetailResponse response = service.getApprovalRequestDetail(REQUEST_ID);

            assertNotNull(response);
            assertNotNull(response.getRequestItems());
            assertEquals(1, response.getRequestItems().size());
            assertNotNull(response.getCurrentTopics());
            assertEquals(1, response.getCurrentTopics().size());
        }

        @Test
        @DisplayName("HT_DY_03 - Đảm bảo Admin xem chi tiết yêu cầu duyệt câu hỏi ôn tập thành công.")
        void getApprovalRequestDetail_Success_QuestionReviewCreate() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic activeTopic = topic(TOPIC_ID, SUBJECT_ID, true);
            Question question = question(QUESTION_ID, TOPIC_ID, false);

            topicStore.put(TOPIC_ID, activeTopic);
            questionStore.put(QUESTION_ID, question);
            answerStore.put(QUESTION_ID, List.of(
                    answer(1L, QUESTION_ID, "A", true, 1, false),
                    answer(2L, QUESTION_ID, "B", false, 2, true)
            ));

            saveApprovalRequest(
                    RequestType.QUESTION_REVIEW_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(QUESTION_ID)
            );

            ApprovalRequestDetailResponse response = service.getApprovalRequestDetail(REQUEST_ID);

            assertNotNull(response);
            assertNotNull(response.getRequestItems());
            assertEquals(1, response.getRequestItems().size());
            assertNotNull(response.getRequestItems().get(0).getQuestion());
            assertEquals(1, response.getRequestItems().get(0).getQuestion().getAnswers().size());
        }

        @Test
        @DisplayName("HT_DY_04 - Đảm bảo báo lỗi khi yêu cầu duyệt không tồn tại.")
        void getApprovalRequestDetail_Fail_ThrowsWhenRequestMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            assertThrows(AppException.class, () -> service.getApprovalRequestDetail(99L));
        }

        @Test
        @DisplayName("HT_DY_05 - Đảm bảo giảng viên không xem được yêu cầu của giảng viên khác.")
        void getApprovalRequestDetail_Fail_TeacherCannotAccessOtherTeacherRequest() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    999L,
                    List.of(CLASSROOM_ID)
            );

            assertThrows(AppException.class, () -> service.getApprovalRequestDetail(REQUEST_ID));
        }
    }

    @Nested
    class SearchApprovalRequestTests {

        @Test
        @DisplayName("HT_DY_06 - Đảm bảo Admin tìm kiếm tất cả yêu cầu duyệt.")
        void searchApprovalRequest_Success_AdminSearchesAll() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            ApprovalRequest requestEntity = approvalRequest(
                    REQUEST_ID,
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID
            );

            when(approvalRequestRepository.searchApprovalRequest(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(requestEntity)));

            ResponseListData<ApprovalRequestSearchResponse> response =
                    service.searchApprovalRequest(searchRequest());

            assertNotNull(response);

            verify(approvalRequestRepository).searchApprovalRequest(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("HT_DY_07 - Đảm bảo Giảng viên tìm kiếm yêu cầu do mình tạo.")
        void searchApprovalRequest_Success_TeacherSearchesOwnRequests() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            ApprovalRequest requestEntity = approvalRequest(
                    REQUEST_ID,
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID
            );

            when(approvalRequestRepository.searchApprovalRequest(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(requestEntity)));

            ResponseListData<ApprovalRequestSearchResponse> response =
                    service.searchApprovalRequest(searchRequest());

            assertNotNull(response);

            verify(approvalRequestRepository).searchApprovalRequest(any(), any(Pageable.class));
        }
    }

    @Nested
    class ApproveRequestTests {

        @Test
        @DisplayName("HT_DY_08 - Đảm bảo Admin duyệt yêu cầu tạo lớp thành công.")
        void approveRequest_Success_ApprovesClassCreateRequest() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Classroom classroom = classroom(CLASSROOM_ID);
            classroomStore.put(CLASSROOM_ID, classroom);

            ApprovalRequest request = saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            service.approveRequest(REQUEST_ID);

            assertEquals(ApprovalStatus.APPROVED, request.getStatus());
            assertEquals(ADMIN_ID, request.getReviewerId());
            assertTrue(classroom.getIsActive());
            assertEquals(classroom, savedClassroom);

            verify(approvalRequestRepository).save(eq(request));
            verify(classroomRepository).save(eq(classroom));
        }

        @Test
        @DisplayName("HT_DY_09 - Đảm bảo Admin duyệt yêu cầu tạo chủ đề thành công.")
        void approveRequest_Success_ApprovesTopicCreateRequest() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic oldTopic = topic(1L, SUBJECT_ID, true);
            Topic firstNewTopic = topic(2L, SUBJECT_ID, false);
            Topic secondNewTopic = topic(3L, SUBJECT_ID, false);

            topicStore.put(1L, oldTopic);
            topicStore.put(2L, firstNewTopic);
            topicStore.put(3L, secondNewTopic);

            ApprovalRequest request = saveApprovalRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(2L, 3L)
            );

            service.approveRequest(REQUEST_ID);

            assertEquals(ApprovalStatus.APPROVED, request.getStatus());
            assertTrue(firstNewTopic.getIsActive());
            assertTrue(secondNewTopic.getIsActive());
            assertEquals(2L, secondNewTopic.getPrerequisiteTopicId());
            assertFalse(oldTopic.getIsActive());

            verify(topicRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("HT_DY_10 - Đảm bảo Admin duyệt câu hỏi vào ngân hàng ôn tập thành công.")
        void approveRequest_Success_ApprovesQuestionReviewCreateRequest() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic activeTopic = topic(TOPIC_ID, SUBJECT_ID, true);
            Question originalQuestion = question(QUESTION_ID, TOPIC_ID, false);

            topicStore.put(TOPIC_ID, activeTopic);
            questionStore.put(QUESTION_ID, originalQuestion);
            answerStore.put(QUESTION_ID, List.of(
                    answer(1L, QUESTION_ID, "A", true, 1, false),
                    answer(2L, QUESTION_ID, "B", false, 2, false),
                    answer(3L, QUESTION_ID, "Deleted", false, 3, true)
            ));

            ApprovalRequest request = saveApprovalRequest(
                    RequestType.QUESTION_REVIEW_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(QUESTION_ID)
            );

            service.approveRequest(REQUEST_ID);

            assertEquals(ApprovalStatus.APPROVED, request.getStatus());

            assertEquals(2, savedQuestions.size());

            Question reviewQuestion = savedQuestions.stream()
                    .filter(question -> Boolean.TRUE.equals(question.getIsReviewQuestion()))
                    .findFirst()
                    .orElseThrow();

            Question updatedOriginalQuestion = savedQuestions.stream()
                    .filter(question -> QUESTION_ID.equals(question.getQuestionId()))
                    .findFirst()
                    .orElseThrow();

            assertTrue(reviewQuestion.getIsReviewQuestion());
            assertFalse(reviewQuestion.getIsAddedToReview());

            assertEquals(originalQuestion, updatedOriginalQuestion);
            assertTrue(updatedOriginalQuestion.getIsAddedToReview());

            assertNotNull(savedAnswer);

            verify(answerRepository, times(2)).save(any(Answer.class));
            verify(questionRepository).save(eq(originalQuestion));
        }

        @Test
        @DisplayName("HT_DY_11 - Đảm bảo chỉ Admin mới được duyệt yêu cầu.")
        void approveRequest_Fail_ThrowsWhenCurrentUserIsNotAdmin() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));

            verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
        }

        @Test
        @DisplayName("HT_DY_12 - Đảm bảo báo lỗi khi duyệt request không tồn tại.")
        void approveRequest_Fail_ThrowsWhenRequestMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            assertThrows(AppException.class, () -> service.approveRequest(99L));
        }

        @Test
        @DisplayName("HT_DY_13 - Đảm bảo không cho duyệt request đã xử lý.")
        void approveRequest_Fail_ThrowsWhenRequestIsNotPending() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.APPROVED,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));
        }

        @Test
        @DisplayName("HT_DY_14 - Đảm bảo duyệt yêu cầu tạo chủ đề thất bại khi không tìm thấy topic hợp lệ.")
        void approveRequest_Fail_ThrowsWhenTopicRequestHasNoTopics() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            saveApprovalRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(999L)
            );

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));
        }

        @Test
        @DisplayName("HT_DY_15 - Đảm bảo không duyệt câu hỏi đã bị xóa.")
        void approveRequest_Fail_ThrowsWhenQuestionIsDeleted() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Question deletedQuestion = question(QUESTION_ID, TOPIC_ID, true);
            questionStore.put(QUESTION_ID, deletedQuestion);

            saveApprovalRequest(
                    RequestType.QUESTION_REVIEW_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(QUESTION_ID)
            );

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));
        }

        @Test
        @DisplayName("HT_DY_16 - Đảm bảo không duyệt câu hỏi khi topic của câu hỏi không active.")
        void approveRequest_Fail_ThrowsWhenQuestionTopicInactive() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic inactiveTopic = topic(TOPIC_ID, SUBJECT_ID, false);
            Question originalQuestion = question(QUESTION_ID, TOPIC_ID, false);

            topicStore.put(TOPIC_ID, inactiveTopic);
            questionStore.put(QUESTION_ID, originalQuestion);

            saveApprovalRequest(
                    RequestType.QUESTION_REVIEW_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(QUESTION_ID)
            );

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));
        }

        @Test
        @DisplayName("HT_DY_17 - Đảm bảo duyệt tạo lớp thất bại khi request không có item classroom.")
        void approveRequest_Fail_ThrowsWhenClassCreateItemMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            ApprovalRequest request = approvalRequest(
                    REQUEST_ID,
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID
            );

            approvalRequestStore.put(REQUEST_ID, request);
            itemStore.put(REQUEST_ID, List.of());

            assertThrows(AppException.class, () -> service.approveRequest(REQUEST_ID));
        }
    }

    @Nested
    class RejectRequestTests {

        @Test
        @DisplayName("HT_DY_18 - Đảm bảo Admin từ chối yêu cầu đang chờ duyệt thành công.")
        void rejectRequest_Success_RejectsPendingRequest() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            ApprovalRequest request = saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            service.rejectRequest(REQUEST_ID, rejectRequest("Invalid request"));

            assertEquals(ApprovalStatus.REJECTED, request.getStatus());
            assertEquals(ADMIN_ID, request.getReviewerId());
            assertEquals("Invalid request", request.getRejectReason());

            verify(approvalRequestRepository).save(eq(request));
        }

        @Test
        @DisplayName("HT_DY_19 - Đảm bảo chỉ Admin mới được từ chối yêu cầu.")
        void rejectRequest_Fail_ThrowsWhenCurrentUserIsNotAdmin() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            assertThrows(AppException.class, () ->
                    service.rejectRequest(REQUEST_ID, rejectRequest("Invalid"))
            );

            verify(approvalRequestRepository, never()).save(any(ApprovalRequest.class));
        }

        @Test
        @DisplayName("HT_DY_20 - Đảm bảo từ chối request không tồn tại sẽ báo lỗi.")
        void rejectRequest_Fail_ThrowsWhenRequestMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            assertThrows(AppException.class, () ->
                    service.rejectRequest(99L, rejectRequest("Invalid"))
            );
        }

        @Test
        @DisplayName("HT_DY_21 - Đảm bảo không từ chối request đã được xử lý.")
        void rejectRequest_Fail_ThrowsWhenRequestIsNotPending() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.APPROVED,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            assertThrows(AppException.class, () ->
                    service.rejectRequest(REQUEST_ID, rejectRequest("Invalid"))
            );
        }

        @Test
        @DisplayName("HT_DY_22 - Đảm bảo validate lý do từ chối không được để trống.")
        void rejectRequest_Fail_ThrowsWhenRejectReasonIsBlank() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            assertThrows(AppException.class, () ->
                    service.rejectRequest(REQUEST_ID, rejectRequest("   "))
            );
        }

        @Test
        @DisplayName("HT_DY_23 - Đảm bảo validate lý do từ chối không được null.")
        void rejectRequest_Fail_ThrowsWhenRejectReasonIsNull() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            saveApprovalRequest(
                    RequestType.CLASS_CREATE,
                    ApprovalStatus.PENDING,
                    TEACHER_ID,
                    List.of(CLASSROOM_ID)
            );

            assertThrows(AppException.class, () ->
                    service.rejectRequest(REQUEST_ID, rejectRequest(null))
            );
        }
    }
}