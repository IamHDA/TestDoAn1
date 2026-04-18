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
    @DisplayName("createRequest - thành công tạo request mới và các items liên quan")
    void createRequest_Success() {
        ApprovalRequest savedRequest = new ApprovalRequest();
        savedRequest.setId(10L);
        savedRequest.setRequestType(RequestType.TOPIC_CREATE);

        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenReturn(savedRequest);

        approvalRequestService.createRequest(RequestType.TOPIC_CREATE, "Description", 2L, List.of(1L, 2L));

        verify(approvalRequestRepository).save(any(ApprovalRequest.class));
        verify(approvalRequestItemsRepository).saveAll(anyList());
    }

    // ===================== getApprovalRequestDetail =====================

    @Test
    @DisplayName("getApprovalRequestDetail - thành công lấy thông tin cho CLASS_CREATE")
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

    // ===================== approveRequest =====================

    @Test
    @DisplayName("approveRequest - CLASS_CREATE thành công khi user là ADMIN")
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
    @DisplayName("approveRequest - TOPIC_CREATE thay đổi status thành APPROVED, active topic và đổi prereqs")
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
    @DisplayName("approveRequest - ném exception khi user không phải ADMIN")
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

    // ===================== rejectRequest =====================

    @Test
    @DisplayName("rejectRequest - thành công set reject và lý do khi ADMIN thao tác")
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
}
