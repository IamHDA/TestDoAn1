package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.approval.ApproveRejectRequest;
import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestDetailResponse;
import com.vn.backend.dto.response.approvalrequest.ApprovalRequestSearchResponse;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.ApprovalRequest;
import com.vn.backend.entities.ApprovalRequestItems;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.ClassroomStatus;
import com.vn.backend.enums.RequestType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnswerRepository;
import com.vn.backend.repositories.ApprovalRequestItemsRepository;
import com.vn.backend.repositories.ApprovalRequestRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.QuestionRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.ApprovalRequestServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalRequestServiceImpl Unit Tests")
class ApprovalRequestServiceImplTest {

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

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private ApprovalRequestServiceImpl approvalRequestService;

    private User adminUser;
    private User teacherUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .role(Role.ADMIN)
                .build();

        teacherUser = User.builder()
                .id(2L)
                .role(Role.TEACHER)
                .build();
    }

    // ===================== createRequest =====================

    @Test
    @DisplayName("[TC_ARS_01] createRequest - thành công tạo request mới và các items liên quan")
    void createRequest_Success() {
        ApprovalRequest savedRequest = new ApprovalRequest();
        savedRequest.setId(10L);
        savedRequest.setRequestType(RequestType.TOPIC_CREATE);

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(savedRequest);

        approvalRequestService.createRequest(RequestType.TOPIC_CREATE, "Description", 2L, List.of(1L, 2L));

        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
        verify(approvalRequestItemsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("[TC_ARS_02] createRequest - thất bại khi requestType null")
    void createRequest_Fail_RequestTypeNull() {
        assertThatThrownBy(() -> approvalRequestService.createRequest(null, "Desc", 2L, List.of(1L)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("[TC_ARS_03] createRequest - thất bại khi requesterId null")
    void createRequest_Fail_RequesterIdNull() {
        assertThatThrownBy(() -> approvalRequestService.createRequest(RequestType.TOPIC_CREATE, "Desc", null, List.of(1L)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("[TC_ARS_04] createRequest - thất bại khi entityIds null hoặc trống")
    void createRequest_Fail_EntityIdsEmpty() {
        assertThatThrownBy(() -> approvalRequestService.createRequest(RequestType.TOPIC_CREATE, "Desc", 2L, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);

        assertThatThrownBy(() -> approvalRequestService.createRequest(RequestType.TOPIC_CREATE, "Desc", 2L, List.of()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    // ===================== getApprovalRequestDetail =====================

    @Test
    @DisplayName("[TC_ARS_05] getApprovalRequestDetail - thành công cho TEACHER")
    void getApprovalRequestDetail_Teacher_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setRequestType(RequestType.CLASS_CREATE);
        request.setRequester(teacherUser);
        request.setItems(List.of(new ApprovalRequestItems()));

        // Teacher should call findByIdWithDetails with their userId
        when(approvalRequestRepository.findByIdWithDetails(10L, 2L)).thenReturn(Optional.of(request));

        Classroom classroom = new Classroom();
        classroom.setClassroomId(100L);
        when(classroomRepository.findById(any())).thenReturn(Optional.of(classroom));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(10L);

        assertThat(response).isNotNull();
        verify(approvalRequestRepository).findByIdWithDetails(10L, 2L);
    }

    @Test
    @DisplayName("[TC_ARS_06] getApprovalRequestDetail - thành công lấy thông tin cho CLASS_CREATE")
    void getApprovalRequestDetail_ClassCreate_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // requesterId will be 2L (since TEACHER)

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setRequestType(RequestType.CLASS_CREATE);
        request.setRequester(teacherUser);

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(100L);
        item.setIsDeleted(false);
        request.setItems(List.of(item));

        when(approvalRequestRepository.findByIdWithDetails(10L, 2L)).thenReturn(Optional.of(request));

        Classroom classroom = new Classroom();
        classroom.setClassroomId(100L);
        classroom.setTeacher(teacherUser);
        classroom.setSubject(new com.vn.backend.entities.Subject());
        classroom.setSchedules(new java.util.ArrayList<>());
        when(classroomRepository.findById(100L)).thenReturn(Optional.of(classroom));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(10L);

        // Verification
        assertThat(response).isNotNull();
        assertThat(response.getRequestItems()).hasSize(1);
        assertThat(response.getRequestItems().get(0).getClassroom()).isNotNull();
    }

    @Test
    @DisplayName("[TC_ARS_07] getApprovalRequestDetail - thành công cho TOPIC_CREATE với prerequisite")
    void getApprovalRequestDetail_TopicCreate_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(11L);
        request.setRequestType(RequestType.TOPIC_CREATE);
        request.setRequester(adminUser);

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(1L);
        item.setIsDeleted(false);
        request.setItems(List.of(item));

        when(approvalRequestRepository.findByIdWithDetails(11L, null)).thenReturn(Optional.of(request));

        Topic topic = new Topic();
        topic.setTopicId(1L);
        topic.setTopicName("Topic 1");
        topic.setSubjectId(50L);
        when(topicRepository.findByTopicIdAndIsDeletedFalse(1L)).thenReturn(topic);

        Topic activeTopic = new Topic();
        activeTopic.setTopicId(2L);
        activeTopic.setTopicName("Active Topic");
        activeTopic.setPrerequisiteTopicId(3L);
        Topic prereq = new Topic(); prereq.setTopicId(3L); prereq.setTopicName("Prereq");
        activeTopic.setPrerequisiteTopic(prereq);

        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(List.of(activeTopic));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(11L);

        assertThat(response).isNotNull();
        assertThat(response.getRequestItems().get(0).getTopicResponse()).isNotNull();
        assertThat(response.getCurrentTopics()).hasSize(1);
        assertThat(response.getCurrentTopics().get(0).getPrerequisiteTopic()).isNotNull();
    }

    @Test
    @DisplayName("[TC_ARS_08] getApprovalRequestDetail - thành công cho QUESTION_REVIEW_CREATE")
    void getApprovalRequestDetail_QuestionReview_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(12L);
        request.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        request.setRequester(adminUser);

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(100L);
        item.setIsDeleted(false);
        request.setItems(List.of(item));

        when(approvalRequestRepository.findByIdWithDetails(12L, null)).thenReturn(Optional.of(request));

        Question question = new Question();
        question.setQuestionId(100L);
        question.setContent("Q Content");
        question.setType(com.vn.backend.enums.QuestionType.SINGLE_CHOICE);
        question.setIsDeleted(false);
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        com.vn.backend.entities.Answer a1 = new com.vn.backend.entities.Answer();
        a1.setAnswerId(1L); a1.setContent("A1"); a1.setIsCorrect(true); a1.setDisplayOrder(1); a1.setIsDeleted(false);
        com.vn.backend.entities.Answer a2 = new com.vn.backend.entities.Answer();
        a2.setAnswerId(2L); a2.setContent("Deleted"); a2.setIsDeleted(true);

        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(List.of(a1, a2));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(12L);

        assertThat(response).isNotNull();
        assertThat(response.getRequestItems().get(0).getQuestion().getAnswers()).hasSize(1); // Only non-deleted
        assertThat(response.getRequestItems().get(0).getQuestion().getAnswers().get(0).getContent()).isEqualTo("A1");
    }

    @Test
    @DisplayName("[TC_ARS_09] getApprovalRequestDetail - xử lý Topic không có prerequisite và subject")
    void getApprovalRequestDetail_TopicCreate_NullFields_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(13L);
        request.setRequestType(RequestType.TOPIC_CREATE);
        request.setRequester(adminUser);

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(1L);
        item.setIsDeleted(false);
        request.setItems(List.of(item));

        when(approvalRequestRepository.findByIdWithDetails(13L, null)).thenReturn(Optional.of(request));

        Topic topic = new Topic();
        topic.setTopicId(1L);
        topic.setSubjectId(50L);
        // Topic has no subject and no prerequisite
        when(topicRepository.findByTopicIdAndIsDeletedFalse(1L)).thenReturn(topic);
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(List.of(topic));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(13L);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentTopics().get(0).getSubjectName()).isNull();
        assertThat(response.getCurrentTopics().get(0).getPrerequisiteTopic()).isNull();
    }

    @Test
    @DisplayName("[TC_ARS_10] getApprovalRequestDetail - xử lý Topic có prereqId nhưng prereqTopic null")
    void getApprovalRequestDetail_TopicCreate_PrereqMismatch_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(15L);
        request.setRequestType(RequestType.TOPIC_CREATE);
        request.setRequester(adminUser);
        request.setItems(List.of(new ApprovalRequestItems()));

        when(approvalRequestRepository.findByIdWithDetails(15L, null)).thenReturn(Optional.of(request));

        Topic topic = new Topic();
        topic.setTopicId(1L);
        topic.setPrerequisiteTopicId(99L);
        topic.setPrerequisiteTopic(null); // ID exists but entity is null

        when(topicRepository.findByTopicIdAndIsDeletedFalse(any())).thenReturn(topic);
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(any())).thenReturn(List.of(topic));

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(15L);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentTopics().get(0).getPrerequisiteTopic()).isNull();
    }

    @Test
    @DisplayName("[TC_ARS_11] getApprovalRequestDetail - xử lý Question null type và null topic")
    void getApprovalRequestDetail_QuestionReview_NullFields_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(14L);
        request.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        request.setRequester(adminUser);
        request.setItems(List.of(new ApprovalRequestItems())); // items will be fetched from repo

        when(approvalRequestRepository.findByIdWithDetails(14L, null)).thenReturn(Optional.of(request));

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(101L);
        item.setIsDeleted(false);
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(14L)).thenReturn(List.of(item));

        Question question = new Question();
        question.setQuestionId(101L);
        question.setType(null); // Null type
        question.setTopic(null); // Null topic
        question.setIsDeleted(false);
        when(questionRepository.findById(101L)).thenReturn(Optional.of(question));
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(101L)).thenReturn(List.of());

        ApprovalRequestDetailResponse response = approvalRequestService.getApprovalRequestDetail(14L);

        assertThat(response).isNotNull();
        assertThat(response.getRequestItems().get(0).getQuestion().getType()).isNull();
        assertThat(response.getRequestItems().get(0).getQuestion().getTopic()).isNull();
    }

    @Test
    @DisplayName("[TC_ARS_12] getApprovalRequestDetail - thất bại khi không tìm thấy ID yêu cầu")
    void getApprovalRequestDetail_Fail_NotFound() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(approvalRequestRepository.findByIdWithDetails(99L, null)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        assertThatThrownBy(() -> approvalRequestService.getApprovalRequestDetail(99L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    // ===================== searchApprovalRequest =====================

    @Test
    @DisplayName("[TC_ARS_13] searchApprovalRequest - thành công cho TEACHER (lọc theo userId)")
    void searchApprovalRequest_Teacher_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);

        com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest filters = new com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest();
        BaseFilterSearchRequest<com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(filters);
        com.vn.backend.dto.request.common.SearchRequest pagination = new com.vn.backend.dto.request.common.SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        request.setPagination(pagination);

        ApprovalRequest entity = new ApprovalRequest();
        entity.setId(10L);
        entity.setRequester(teacherUser);
        Page<ApprovalRequest> page = new PageImpl<>(List.of(entity));
        when(approvalRequestRepository.searchApprovalRequest(any(), any())).thenReturn(page);

        ResponseListData<ApprovalRequestSearchResponse> response = approvalRequestService.searchApprovalRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(approvalRequestRepository).searchApprovalRequest(argThat(dto -> dto.getUserId() != null && dto.getUserId().equals(2L)), any());
    }

    @Test
    @DisplayName("[TC_ARS_14] searchApprovalRequest - thành công cho ADMIN (không lọc theo userId)")
    void searchApprovalRequest_Admin_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest filters = new com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest();
        BaseFilterSearchRequest<com.vn.backend.dto.request.approval.ApprovalRequestSearchRequest> request = new BaseFilterSearchRequest<>();
        request.setFilters(filters);
        com.vn.backend.dto.request.common.SearchRequest pagination = new com.vn.backend.dto.request.common.SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        request.setPagination(pagination);

        ApprovalRequest entity = new ApprovalRequest();
        entity.setId(10L);
        entity.setRequester(teacherUser);
        Page<ApprovalRequest> page = new PageImpl<>(List.of(entity));
        when(approvalRequestRepository.searchApprovalRequest(any(), any())).thenReturn(page);

        ResponseListData<ApprovalRequestSearchResponse> response = approvalRequestService.searchApprovalRequest(request);

        assertThat(response).isNotNull();
        verify(approvalRequestRepository).searchApprovalRequest(argThat(dto -> dto.getUserId() == null), any());
    }

    // ===================== approveRequest =====================

    @Test
    @DisplayName("[TC_ARS_15] approveRequest - CLASS_CREATE thành công khi user là ADMIN")
    void approveRequest_ClassCreate_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.CLASS_CREATE);

        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(100L); // classroomId
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(10L)).thenReturn(List.of(item));

        Classroom classroom = new Classroom();
        classroom.setClassroomId(100L);
        classroom.setClassroomStatus(ClassroomStatus.ACTIVE);
        when(classroomRepository.findByClassroomIdAndClassroomStatus(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(classroom));

        approvalRequestService.approveRequest(10L);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(classroom.getIsActive()).isTrue();
        verify(classroomRepository).save(classroom);
        verify(approvalRequestRepository).save(request);
    }

    @Test
    @DisplayName("[TC_ARS_16] approveRequest - CLASS_CREATE thất bại khi danh sách items trống")
    void approveRequest_ClassCreate_Fail_ItemsEmpty() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.CLASS_CREATE);

        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(10L)).thenReturn(List.of()); // Empty
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        assertThatThrownBy(() -> approvalRequestService.approveRequest(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("[TC_ARS_17] approveRequest - QUESTION_REVIEW_CREATE thành công và clone dữ liệu")
    void approveRequest_QuestionReview_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(20L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(100L);
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(20L)).thenReturn(List.of(item));

        Question original = new Question();
        original.setQuestionId(100L);
        original.setTopicId(50L);
        original.setIsDeleted(false);
        when(questionRepository.findById(100L)).thenReturn(Optional.of(original));

        Topic topic = new Topic();
        topic.setIsActive(true);
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(Optional.of(topic));

        Question savedReview = new Question();
        savedReview.setQuestionId(200L);
        when(questionRepository.save(any(Question.class))).thenReturn(savedReview);

        com.vn.backend.entities.Answer a1 = new com.vn.backend.entities.Answer();
        a1.setAnswerId(1L); a1.setIsDeleted(false);
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(List.of(a1));

        approvalRequestService.approveRequest(20L);

        // Check if original is marked as added to review
        assertThat(original.getIsAddedToReview()).isTrue();
        verify(questionRepository, times(2)).save(any(Question.class)); // 1 for new review, 1 for original update
        verify(answerRepository).save(any(com.vn.backend.entities.Answer.class));
    }

    @Test
    @DisplayName("[TC_ARS_18] approveRequest - QUESTION_REVIEW_CREATE bỏ qua đáp án đã xóa")
    void approveRequest_QuestionReview_SkipDeletedAnswers() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        ApprovalRequest request = new ApprovalRequest();
        request.setId(21L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(100L);
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(21L)).thenReturn(List.of(item));

        Question original = new Question();
        original.setQuestionId(100L);
        original.setTopicId(50L);
        original.setIsDeleted(false);
        when(questionRepository.findById(100L)).thenReturn(Optional.of(original));
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(Optional.of(new Topic()));
        when(questionRepository.save(any(Question.class))).thenReturn(new Question());

        com.vn.backend.entities.Answer a1 = new com.vn.backend.entities.Answer();
        a1.setAnswerId(1L); a1.setIsDeleted(true); // Deleted answer
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(100L)).thenReturn(List.of(a1));

        approvalRequestService.approveRequest(21L);

        verify(answerRepository, never()).save(any(com.vn.backend.entities.Answer.class));
    }

    @Test
    @DisplayName("[TC_ARS_19] approveRequest - TOPIC_CREATE thay đổi status thành APPROVED, active topic và đổi prereqs")
    void approveRequest_TopicCreate_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item1 = new ApprovalRequestItems();
        item1.setEntityId(1L);
        item1.setCreatedAt(java.time.LocalDateTime.now());
        ApprovalRequestItems item2 = new ApprovalRequestItems();
        item2.setEntityId(2L);
        item2.setCreatedAt(java.time.LocalDateTime.now());
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(10L)).thenReturn(new ArrayList<>(List.of(item1, item2)));

        Topic t1 = new Topic(); t1.setTopicId(1L); t1.setSubjectId(50L);
        Topic t2 = new Topic(); t2.setTopicId(2L); t2.setSubjectId(50L);
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(anyList())).thenReturn(List.of(t1, t2));
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(new ArrayList<>()); // No old active topics

        approvalRequestService.approveRequest(10L);

        // Verification
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(t1.getIsActive()).isTrue();
        assertThat(t2.getIsActive()).isTrue();
        assertThat(t2.getPrerequisiteTopicId()).isEqualTo(1L); // The prereq chaining
        verify(topicRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("[TC_ARS_20] approveRequest - TOPIC_CREATE thành công và deactive các topic cũ")
    void approveRequest_TopicCreate_WithDeactivation() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item1 = new ApprovalRequestItems();
        item1.setEntityId(1L);
        item1.setCreatedAt(java.time.LocalDateTime.now());
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(10L)).thenReturn(new ArrayList<>(List.of(item1)));

        Topic t1 = new Topic(); t1.setTopicId(1L); t1.setSubjectId(50L);
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(List.of(1L))).thenReturn(List.of(t1));

        // Old topic that should be deactivated
        Topic oldTopic = new Topic();
        oldTopic.setTopicId(5L);
        oldTopic.setIsActive(true);
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(List.of(oldTopic));

        approvalRequestService.approveRequest(10L);

        assertThat(oldTopic.getIsActive()).isFalse();
        verify(topicRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("[TC_ARS_21] approveRequest - TOPIC_CREATE với chỉ 1 topic (không có prereq chain)")
    void approveRequest_TopicCreate_SingleTopic() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        ApprovalRequest request = new ApprovalRequest();
        request.setId(30L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(request));

        ApprovalRequestItems item = new ApprovalRequestItems();
        item.setEntityId(1L);
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(30L)).thenReturn(new ArrayList<>(List.of(item)));

        Topic t1 = new Topic(); t1.setTopicId(1L); t1.setSubjectId(50L);
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(List.of(1L))).thenReturn(List.of(t1));
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(50L)).thenReturn(List.of());

        approvalRequestService.approveRequest(30L);

        assertThat(t1.getIsActive()).isTrue();
        assertThat(t1.getPrerequisiteTopicId()).isNull();
    }

    @Test
    @DisplayName("[TC_ARS_22] approveRequest - xử lý default case cho unknown type (log warning)")
    void approveRequest_UnknownType_LogWarning() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        // We use a mock to "simulate" an unknown type if possible,
        // but since it's an enum, we just ensure existing types are covered.
        // To hit the default branch, we'd need a mock that returns something else.
        ApprovalRequest request = mock(ApprovalRequest.class);
        when(request.getStatus()).thenReturn(ApprovalStatus.PENDING);
        // Let's assume there's a way to have a null or mock type
        when(request.getRequestType()).thenReturn(null);

        when(approvalRequestRepository.findByIdAndIsDeletedFalse(40L)).thenReturn(Optional.of(request));

        approvalRequestService.approveRequest(40L);

        verify(approvalRequestRepository).save(request);
    }

    @Test
    @DisplayName("[TC_ARS_23] approveRequest - TOPIC_CREATE thất bại khi không tìm thấy topics")
    void approveRequest_TopicCreate_Fail_NoTopicsFound() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(1L);
        request.setStatus(ApprovalStatus.PENDING);
        request.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(request));
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(1L)).thenReturn(new ArrayList<>(List.of(new ApprovalRequestItems())));
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(anyList())).thenReturn(List.of()); // Empty

        assertThatThrownBy(() -> approvalRequestService.approveRequest(1L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("[TC_ARS_24] approveRequest - ném exception khi user không phải ADMIN")
    void approveRequest_ThrowsException_WhenNotAdmin() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // Not admin
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> approvalRequestService.approveRequest(10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("[TC_ARS_25] approveRequest - thất bại khi yêu cầu không tồn tại")
    void approveRequest_Fail_NotFound() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not Found");

        assertThatThrownBy(() -> approvalRequestService.approveRequest(99L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("[TC_ARS_26] approveRequest - thất bại khi trạng thái yêu cầu không phải PENDING")
    void approveRequest_Fail_InvalidStatus() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        ApprovalRequest request = new ApprovalRequest();
        request.setStatus(ApprovalStatus.APPROVED); // Not PENDING
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> approvalRequestService.approveRequest(10L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.INVALID_LOGIC_QUESTION);
    }

    // ===================== rejectRequest =====================

    @Test
    @DisplayName("[TC_ARS_27] rejectRequest - thành công set reject và lý do khi ADMIN thao tác")
    void rejectRequest_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);

        ApprovalRequest request = new ApprovalRequest();
        request.setId(10L);
        request.setStatus(ApprovalStatus.PENDING);

        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        ApproveRejectRequest payload = new ApproveRejectRequest();
        payload.setRejectReason("Not valid info");

        approvalRequestService.rejectRequest(10L, payload);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("Not valid info");
        verify(approvalRequestRepository).save(request);
    }

    @Test
    @DisplayName("[TC_ARS_28] rejectRequest - ném exception khi user không phải ADMIN")
    void rejectRequest_Fail_NotAdmin() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> approvalRequestService.rejectRequest(10L, new ApproveRejectRequest()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.UNAUTHORIZED);
    }

    @Test
    @DisplayName("[TC_ARS_29] rejectRequest - thất bại khi thiếu lý do từ chối")
    void rejectRequest_Fail_MissingReason() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        ApprovalRequest request = new ApprovalRequest();
        request.setStatus(ApprovalStatus.PENDING);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(request));

        ApproveRejectRequest payload = new ApproveRejectRequest();
        payload.setRejectReason(" "); // Empty

        assertThatThrownBy(() -> approvalRequestService.rejectRequest(10L, payload))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.INVALID_LOGIC_QUESTION);
    }
}