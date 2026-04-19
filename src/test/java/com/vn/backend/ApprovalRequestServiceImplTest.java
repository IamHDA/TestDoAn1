package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.approval.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApprovalRequestServiceImpl Unit Tests")
class ApprovalRequestServiceImplTest {

    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private ApprovalRequestItemsRepository approvalRequestItemsRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private AuthService authService;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private ApprovalRequestServiceImpl approvalRequestService;

    private User adminUser;
    private User teacherUser;
    private ApprovalRequest pendingRequest;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).role(Role.ADMIN).build();
        teacherUser = User.builder().id(2L).role(Role.TEACHER).build();
        
        pendingRequest = ApprovalRequest.builder()
                .id(100L)
                .requestType(RequestType.CLASS_CREATE)
                .status(ApprovalStatus.PENDING)
                .requesterId(2L)
                .build();
                
        when(messageUtils.getMessage(anyString())).thenReturn("Error message");
        when(authService.getCurrentUser()).thenReturn(adminUser);
    }

    // ================== createRequest ==================
    @Test
    @DisplayName("TC_QLLH_APP_01: createRequest - ném exception khi thiếu tham số đầu vào")
    void createRequest_ThrowsException_MissingParams() {
        assertThatThrownBy(() -> approvalRequestService.createRequest(null, "Desc", 1L, List.of(1L)))
                .isInstanceOf(AppException.class);
        
        assertThatThrownBy(() -> approvalRequestService.createRequest(RequestType.CLASS_CREATE, "Desc", null, List.of(1L)))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> approvalRequestService.createRequest(RequestType.CLASS_CREATE, "Desc", 1L, null))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_APP_02: createRequest - thành công và lưu đủ Items")
    void createRequest_Success() {
        approvalRequestService.createRequest(RequestType.CLASS_CREATE, "Tạo lớp", 2L, List.of(500L));
        
        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
        verify(approvalRequestItemsRepository).saveAll(anyList());
    }

    // ================== getApprovalRequestDetail ==================
    @Test
    @DisplayName("TC_QLLH_APP_03: getApprovalRequestDetail - ném 404 khi không thấy request")
    void getDetail_NotFound() {
        when(approvalRequestRepository.findByIdWithDetails(anyLong(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> approvalRequestService.getApprovalRequestDetail(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("TC_QLLH_APP_04: getApprovalRequestDetail - Case CLASS_CREATE thành công")
    void getDetail_ClassCreate_Success() {
        ApprovalRequestItems item = ApprovalRequestItems.builder().entityId(50L).build();
        pendingRequest.setItems(List.of(item));
        
        when(approvalRequestRepository.findByIdWithDetails(eq(100L), any())).thenReturn(Optional.of(pendingRequest));
        when(classroomRepository.findById(50L)).thenReturn(Optional.of(Classroom.builder().classroomId(50L).build()));

        var result = approvalRequestService.getApprovalRequestDetail(100L);
        assertThat(result.getRequestType()).isEqualTo(RequestType.CLASS_CREATE);
    }

    @Test
    @DisplayName("TC_QLLH_APP_14: getApprovalRequestDetail - Case TOPIC_CREATE thành công kèm danh sách topic hiện tại")
    void getDetail_TopicCreate_Success() {
        pendingRequest.setRequestType(RequestType.TOPIC_CREATE);
        Topic newTopic = Topic.builder().topicId(1L).subjectId(10L).topicName("New Topic").build();
        ApprovalRequestItems item = ApprovalRequestItems.builder().entityId(1L).build();
        pendingRequest.setItems(List.of(item));

        when(approvalRequestRepository.findByIdWithDetails(eq(100L), any())).thenReturn(Optional.of(pendingRequest));
        when(topicRepository.findByTopicIdAndIsDeletedFalse(1L)).thenReturn(newTopic);
        // Giả lập trong môn học đang có 1 topic active từ trước
        Topic activeOldTopic = Topic.builder().topicId(99L).topicName("Old Topic").build();
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(List.of(activeOldTopic));

        var result = approvalRequestService.getApprovalRequestDetail(100L);
        
        assertThat(result.getRequestItems()).hasSize(1);
        assertThat(result.getCurrentTopics()).hasSize(1);
        assertThat(result.getCurrentTopics().get(0).getTopicName()).isEqualTo("Old Topic");
    }

    @Test
    @DisplayName("TC_QLLH_APP_15: getApprovalRequestDetail - Case QUESTION_REVIEW_CREATE thành công kèm Answers sắp xếp")
    void getDetail_QuestionReview_Success() {
        pendingRequest.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        ApprovalRequestItems item = ApprovalRequestItems.builder().entityId(1000L).build();
        pendingRequest.setItems(List.of(item));

        Question question = Question.builder().questionId(1000L).content("Q?").isDeleted(false).build();
        when(approvalRequestRepository.findByIdWithDetails(eq(100L), any())).thenReturn(Optional.of(pendingRequest));
        when(questionRepository.findById(1000L)).thenReturn(Optional.of(question));
        
        // Mock 2 câu trả lời với display order khác nhau để test logic Sort
        Answer ans2 = Answer.builder().answerId(2L).content("Ans 2").displayOrder(2).build();
        Answer ans1 = Answer.builder().answerId(1L).content("Ans 1").displayOrder(1).build();
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L)).thenReturn(List.of(ans2, ans1));

        var result = approvalRequestService.getApprovalRequestDetail(100L);

        assertThat(result.getRequestItems().get(0).getQuestion().getAnswers()).hasSize(2);
        // Phải được sort lại theo display order (Ans 1 trước Ans 2)
        assertThat(result.getRequestItems().get(0).getQuestion().getAnswers().get(0).getContent()).isEqualTo("Ans 1");
    }

    @Test
    @DisplayName("TC_QLLH_APP_16: approveTopicRequest - Phải vô hiệu hóa các Topics cũ không nằm trong request")
    void approve_TopicCreate_DeactivateOldTopics() {
        pendingRequest.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));

        Topic tNew = Topic.builder().topicId(1L).subjectId(10L).build();
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(List.of(ApprovalRequestItems.builder().entityId(1L).build()));
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(anyList())).thenReturn(List.of(tNew));

        // Topic cũ ID=99 đang active
        Topic tOld = Topic.builder().topicId(99L).subjectId(10L).isActive(true).build();
        when(topicRepository.findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(10L)).thenReturn(List.of(tOld));

        approvalRequestService.approveRequest(100L);

        verify(topicRepository).saveAll(argThat(list -> ((List<Topic>) list).stream().anyMatch(t -> t.getTopicId() == 99L && !t.getIsActive())));
    }

    @Test
    @DisplayName("TC_QLLH_APP_17: approveQuestionRequest - ném lỗi khi Topic của câu hỏi không còn Active")
    void approve_QuestionReview_TopicInactive() {
        pendingRequest.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(List.of(ApprovalRequestItems.builder().entityId(1000L).build()));

        Question original = Question.builder().questionId(1000L).topicId(5L).isDeleted(false).build();
        when(questionRepository.findById(1000L)).thenReturn(Optional.of(original));
        // Giả lập Topic không tìm thấy hoặc bị xóa
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvalRequestService.approveRequest(100L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_APP_18: acceptCreateClassroomRequest - ném lỗi 404 khi items của request trống")
    void approve_ClassCreate_NoItems() {
        pendingRequest.setRequestType(RequestType.CLASS_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        // Mock list items rỗng
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> approvalRequestService.approveRequest(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }


    // ================== approveRequest ==================
    @Test
    @DisplayName("TC_QLLH_APP_05: approveRequest - ném FORBIDDEN khi không phải ADMIN")
    void approve_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        assertThatThrownBy(() -> approvalRequestService.approveRequest(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_APP_06: approveRequest - ném BAD_REQUEST khi status không phải PENDING")
    void approve_InvalidStatus() {
        pendingRequest.setStatus(ApprovalStatus.APPROVED);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        
        assertThatThrownBy(() -> approvalRequestService.approveRequest(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC_QLLH_APP_07: approveRequest - CLASS_CREATE thành công (Active=true)")
    void approve_ClassCreate_Success() {
        pendingRequest.setRequestType(RequestType.CLASS_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        
        ApprovalRequestItems item = ApprovalRequestItems.builder().entityId(500L).build();
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));
        
        Classroom classroom = Classroom.builder().classroomId(500L).build();
        when(classroomRepository.findByClassroomIdAndClassroomStatus(500L, ClassroomStatus.ACTIVE)).thenReturn(Optional.of(classroom));

        approvalRequestService.approveRequest(100L);

        assertThat(classroom.getIsActive()).isTrue();
        assertThat(pendingRequest.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        verify(classroomRepository).save(classroom);
    }

    @Test
    @DisplayName("TC_QLLH_APP_08: approveRequest - TOPIC_CREATE thành công (Active và set Prerequisite)")
    void approve_TopicCreate_Success() {
        pendingRequest.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));

        Topic t1 = Topic.builder().topicId(1L).subjectId(10L).build();
        Topic t2 = Topic.builder().topicId(2L).subjectId(10L).build();
        
        ApprovalRequestItems item1 = ApprovalRequestItems.builder().entityId(1L).build();
        ApprovalRequestItems item2 = ApprovalRequestItems.builder().entityId(2L).build();
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(List.of(item1, item2));
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(anyList())).thenReturn(List.of(t1, t2));

        approvalRequestService.approveRequest(100L);

        assertThat(t1.getIsActive()).isTrue();
        assertThat(t2.getIsActive()).isTrue();
        assertThat(t2.getPrerequisiteTopicId()).isEqualTo(1L); // Logic chuỗi Topic
        verify(topicRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("TC_QLLH_APP_09: approveRequest - TOPIC_CREATE ném lỗi khi không tìm thấy Topic")
    void approve_TopicCreate_NoFound() {
        pendingRequest.setRequestType(RequestType.TOPIC_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        when(topicRepository.findByTopicIdInAndIsDeletedFalse(anyList())).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> approvalRequestService.approveRequest(100L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_APP_10: approveRequest - QUESTION_REVIEW_CREATE thành công (Clone Question & Answers)")
    void approve_QuestionReview_Success() {
        pendingRequest.setRequestType(RequestType.QUESTION_REVIEW_CREATE);
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));

        ApprovalRequestItems item = ApprovalRequestItems.builder().entityId(1000L).build();
        when(approvalRequestItemsRepository.findByRequestIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));

        Question qOriginal = Question.builder().questionId(1000L).topicId(5L).isDeleted(false).build();
        when(questionRepository.findById(1000L)).thenReturn(Optional.of(qOriginal));
        when(topicRepository.findByTopicIdAndIsActiveTrueAndIsDeletedFalse(5L)).thenReturn(Optional.of(new Topic()));
        
        // Trả về clone question với Id mới
        Question qClone = Question.builder().questionId(2000L).build();
        when(questionRepository.save(any(Question.class))).thenReturn(qClone);
        
        Answer ans = Answer.builder().answerId(1L).build();
        when(answerRepository.findByQuestionIdOrderByDisplayOrder(1000L)).thenReturn(List.of(ans));

        approvalRequestService.approveRequest(100L);

        verify(questionRepository, times(2)).save(any(Question.class)); // 1 lần clone, 1 lần update original
        verify(answerRepository).save(any(Answer.class)); // Lưu answer clone
        assertThat(qOriginal.getIsAddedToReview()).isTrue();
    }

    // ================== rejectRequest ==================
    @Test
    @DisplayName("TC_QLLH_APP_11: rejectRequest - thành công kèm lý do")
    void reject_Success() {
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        
        ApproveRejectRequest req = new ApproveRejectRequest();
        req.setRejectReason("Dữ liệu không hợp lệ");

        approvalRequestService.rejectRequest(100L, req);

        assertThat(pendingRequest.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(pendingRequest.getRejectReason()).isEqualTo("Dữ liệu không hợp lệ");
        verify(approvalRequestRepository).save(pendingRequest);
    }

    @Test
    @DisplayName("TC_QLLH_APP_12: rejectRequest - ném exception khi thiếu lý do")
    void reject_MissingReason() {
        when(approvalRequestRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(pendingRequest));
        
        ApproveRejectRequest req = new ApproveRejectRequest();
        req.setRejectReason(""); // Rỗng

        assertThatThrownBy(() -> approvalRequestService.rejectRequest(100L, req))
                .isInstanceOf(AppException.class);
    }

    // ================== searchApprovalRequest ==================
    @Test
    @DisplayName("TC_QLLH_APP_13: searchApprovalRequest - thành công phân trang")
    void search_Success() {
        ApprovalRequestSearchRequest filters = new ApprovalRequestSearchRequest();
        BaseFilterSearchRequest<ApprovalRequestSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filters);
        req.setPagination(new SearchRequest());

        Page<ApprovalRequest> page = new PageImpl<>(List.of(pendingRequest));
        when(approvalRequestRepository.searchApprovalRequest(any(), any(Pageable.class))).thenReturn(page);

        var result = approvalRequestService.searchApprovalRequest(req);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }
}
