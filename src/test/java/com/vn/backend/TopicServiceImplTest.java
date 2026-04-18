package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.topic.CreateApprovalTopicRequest;
import com.vn.backend.dto.request.topic.CreateTopicRequest;
import com.vn.backend.dto.request.topic.UpdateTopicRequest;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.EntityType;
import com.vn.backend.enums.RequestType;
import com.vn.backend.enums.Role;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ApprovalRequestItemsRepository;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.repositories.TopicRepository;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.TopicServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopicServiceImpl Unit Tests")
class TopicServiceImplTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ApprovalRequestItemsRepository approvalRequestItemsRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ApprovalRequestService approvalRequestService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private TopicServiceImpl topicService;

    private User teacherUser;
    private User adminUser;
    private Topic existingTopic;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .username("teacher")
                .role(Role.TEACHER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        existingTopic = Topic.builder()
                .topicId(100L)
                .topicName("Math Advanced")
                .subjectId(10L)
                .isDeleted(false)
                .build();
    }

    // ===================== approvalRequestTopic =====================

    @Test
    @DisplayName("approvalRequestTopic - ném exception khi user không phải TEACHER")
    void approvalRequestTopic_ThrowsException_WhenNotTeacher() {
        when(authService.getCurrentUser()).thenReturn(adminUser); // ADMIN thì không được tạo request (theo logic hiện tại)
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("approvalRequestTopic - ném exception khi Subject không tồn tại")
    void approvalRequestTopic_ThrowsException_WhenSubjectNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(10L)).thenReturn(false);
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("approvalRequestTopic - ném exception khi topicRequests trống")
    void approvalRequestTopic_ThrowsException_WhenTopicRequestsEmpty() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(10L)).thenReturn(true);

        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setTopicRequests(new ArrayList<>()); // rỗng

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.INVALID_LOGIC_QUESTION);
                });
    }

    @Test
    @DisplayName("approvalRequestTopic - thành công")
    void approvalRequestTopic_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(10L)).thenReturn(true);
        when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(RequestType.TOPIC_CREATE, ApprovalStatus.PENDING, EntityType.TOPIC))
                .thenReturn(new ArrayList<>()); // không có pending request

        // Mô phỏng saveAllTopic trả về topic đã có ID
        when(topicRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> {
            List<Topic> topics = inv.getArgument(0);
            topics.forEach(t -> t.setTopicId(999L));
            return topics;
        });

        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setRequestType(RequestType.TOPIC_CREATE);
        request.setRequestDescription("Create new topics");

        CreateTopicRequest tRequest = new CreateTopicRequest();
        tRequest.setTopicName("New Topic");
        request.setTopicRequests(List.of(tRequest));

        topicService.approvalRequestTopic(request);

        verify(approvalRequestService).createRequest(
                eq(RequestType.TOPIC_CREATE),
                eq("Create new topics"),
                eq(teacherUser.getId()),
                anyList()
        );
    }

    // ===================== updateTopic =====================

    @Test
    @DisplayName("updateTopic - thành công khi user là ADMIN")
    void updateTopic_Success_WhenAdmin() {
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(authService.getCurrentUser()).thenReturn(adminUser);

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setTopicName("Updated Topic Name");

        topicService.updateTopic(100L, request);

        assertThat(existingTopic.getTopicName()).isEqualTo("Updated Topic Name");
        verify(topicRepository).save(existingTopic);
    }

    @Test
    @DisplayName("updateTopic - ném exception khi user không phải ADMIN")
    void updateTopic_ThrowsException_WhenNotAdmin() {
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setTopicName("Updated Name");

        assertThatThrownBy(() -> topicService.updateTopic(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.UNAUTHORIZED);
                });
    }
    
    @Test
    @DisplayName("updateTopic - ném exception khi set topic tự làm prerequisite của chính nó")
    void updateTopic_ThrowsException_WhenPrerequisiteIsItself() {
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(authService.getCurrentUser()).thenReturn(adminUser);

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setPrerequisiteTopicId(100L); // Trùng ID với topic đang update

        assertThatThrownBy(() -> topicService.updateTopic(100L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ===================== deleteTopic =====================

    @Test
    @DisplayName("deleteTopic - thành công (soft delete) khi user là ADMIN")
    void deleteTopic_Success() {
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(authService.getCurrentUser()).thenReturn(adminUser);

        topicService.deleteTopic(100L);

        assertThat(existingTopic.getIsDeleted()).isTrue();
        verify(topicRepository).save(existingTopic);
    }

    @Test
    @DisplayName("deleteTopic - ném exception khi user không phải ADMIN")
    void deleteTopic_ThrowsException_WhenNotAdmin() {
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(messageUtils.getMessage(AppConst.MessageConst.UNAUTHORIZED)).thenReturn("Unauthorized");

        assertThatThrownBy(() -> topicService.deleteTopic(100L))
                .isInstanceOf(AppException.class);

        verify(topicRepository, never()).save(any());
    }
}
