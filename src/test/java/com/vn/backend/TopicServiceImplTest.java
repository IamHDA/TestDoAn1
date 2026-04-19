package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.topic.*;
import com.vn.backend.entities.Topic;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TopicServiceImpl Unit Tests")
class TopicServiceImplTest {

    @Mock private TopicRepository topicRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ApprovalRequestItemsRepository approvalRequestItemsRepository;
    @Mock private AuthService authService;
    @Mock private ApprovalRequestService approvalRequestService;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private TopicServiceImpl topicService;

    private User teacherUser;
    private User adminUser;
    private Topic existingTopic;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).role(Role.TEACHER).build();
        adminUser = User.builder().id(2L).role(Role.ADMIN).build();
        existingTopic = Topic.builder().topicId(100L).topicName("Topic 1").subjectId(10L).isDeleted(false).build();
        
        when(messageUtils.getMessage(anyString())).thenReturn("Error message");
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(anyLong())).thenReturn(true);
    }

    // ================== approvalRequestTopic ==================
    @Test
    @DisplayName("TC_QLLH_TOP_01: approvalRequestTopic - ném lỗi khi user không phải TEACHER")
    void approvalRequestTopic_NotTeacher() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        assertThatThrownBy(() -> topicService.approvalRequestTopic(new CreateApprovalTopicRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_02: approvalRequestTopic - ném lỗi khi đang có một request PENDING cho cùng môn học")
    void approvalRequestTopic_PendingExists() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setTopicRequests(List.of(new CreateTopicRequest()));

        // Giả lập đang có topic ID=100 đang nằm trong Request PENDING
        when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(any(), any(), any())).thenReturn(List.of(100L));
        when(topicRepository.findAllById(anyList())).thenReturn(List.of(existingTopic));

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Đang tồn tại một yêu cầu");
    }

    @Test
    @DisplayName("TC_QLLH_TOP_03: approvalRequestTopic - ném lỗi khi tên Topic rỗng")
    void approvalRequestTopic_EmptyName() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        CreateTopicRequest topReq = new CreateTopicRequest();
        topReq.setTopicName(""); // Rỗng
        request.setTopicRequests(List.of(topReq));

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_04: approvalRequestTopic - thành công tạo lô Topics lưu tạm và tạo Request")
    void approvalRequestTopic_Success() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setRequestType(RequestType.TOPIC_CREATE);
        
        CreateTopicRequest topReq = new CreateTopicRequest();
        topReq.setTopicName("Topic New");
        request.setTopicRequests(List.of(topReq));

        when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(any(), any(), any())).thenReturn(Collections.emptyList());
        when(topicRepository.saveAllAndFlush(anyList())).thenReturn(List.of(existingTopic));

        topicService.approvalRequestTopic(request);

        verify(topicRepository).saveAllAndFlush(anyList());
        verify(approvalRequestService).createRequest(eq(RequestType.TOPIC_CREATE), any(), eq(1L), anyList());
    }

    @Test
    @DisplayName("TC_QLLH_TOP_04A: approvalRequestTopic - nÃ©m lá»—i khi mÃ´n há»c khÃ´ng tá»“n táº¡i")
    void approvalRequestTopic_SubjectNotFound() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(999L);
        request.setTopicRequests(List.of(new CreateTopicRequest()));
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(999L)).thenReturn(false);

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_04B: approvalRequestTopic - nÃ©m lá»—i khi danh sÃ¡ch topic rá»—ng")
    void approvalRequestTopic_EmptyTopicRequests() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setTopicRequests(Collections.emptyList());

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.INVALID_LOGIC_QUESTION);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_04C: approvalRequestTopic - nÃ©m lá»—i khi topicId tham chiáº¿u khÃ´ng tá»“n táº¡i")
    void approvalRequestTopic_ExistingTopicNotFound() {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(10L);
        request.setRequestType(RequestType.TOPIC_CREATE);

        CreateTopicRequest topReq = new CreateTopicRequest();
        topReq.setTopicId(999L);
        topReq.setTopicName("Reuse Topic");
        request.setTopicRequests(List.of(topReq));

        when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(any(), any(), any())).thenReturn(Collections.emptyList());
        when(topicRepository.findByTopicIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.approvalRequestTopic(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    // ================== updateTopic ==================
    @Test
    @DisplayName("TC_QLLH_TOP_05: updateTopic - ném UNAUTHORIZED nếu không phải ADMIN")
    void updateTopic_NotAdmin() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        assertThatThrownBy(() -> topicService.updateTopic(100L, new UpdateTopicRequest()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_06: updateTopic - ném lỗi khi set chính mình làm prerequisite")
    void updateTopic_SelfPrerequisite() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        
        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setPrerequisiteTopicId(100L); // Trùng với topicId đang sửa

        assertThatThrownBy(() -> topicService.updateTopic(100L, request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_07: updateTopic - thành công cập nhật tên và topic tiên quyết")
    void updateTopic_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        
        // Mock topic tiên quyết (ID=50) tồn tại
        Topic prereq = Topic.builder().topicId(50L).build();
        when(topicRepository.findByTopicIdAndIsDeleted(50L, false)).thenReturn(Optional.of(prereq));

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setTopicName("Topic Updated");
        request.setPrerequisiteTopicId(50L);

        topicService.updateTopic(100L, request);

        assertThat(existingTopic.getTopicName()).isEqualTo("Topic Updated");
        assertThat(existingTopic.getPrerequisiteTopicId()).isEqualTo(50L);
        verify(topicRepository).save(existingTopic);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_07A: updateTopic - nÃ©m lá»—i khi topic khÃ´ng tá»“n táº¡i")
    void updateTopic_NotFound() {
        when(topicRepository.findByTopicIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.updateTopic(999L, new UpdateTopicRequest()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_07B: updateTopic - nÃ©m lá»—i khi prerequisite khÃ´ng tá»“n táº¡i")
    void updateTopic_PrerequisiteNotFound() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));
        when(topicRepository.findByTopicIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setPrerequisiteTopicId(999L);

        assertThatThrownBy(() -> topicService.updateTopic(100L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    // ================== deleteTopic ==================
    @Test
    @DisplayName("TC_QLLH_TOP_08: deleteTopic - thành công xóa mềm (với quyền Admin)")
    void deleteTopic_Success() {
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));

        topicService.deleteTopic(100L);

        assertThat(existingTopic.getIsDeleted()).isTrue();
        verify(topicRepository).save(existingTopic);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_09: deleteTopic - ném lỗi khi không phải Admin")
    void deleteTopic_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(topicRepository.findByTopicIdAndIsDeleted(100L, false)).thenReturn(Optional.of(existingTopic));

        assertThatThrownBy(() -> topicService.deleteTopic(100L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_TOP_09A: deleteTopic - nÃ©m lá»—i khi topic khÃ´ng tá»“n táº¡i")
    void deleteTopic_NotFound() {
        when(topicRepository.findByTopicIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.deleteTopic(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }

    // ================== searchTopic ==================
    @Test
    @DisplayName("TC_QLLH_TOP_10: searchTopics - thành công và lấy đúng data")
    void searchTopics_Success() {
        TopicFilterRequest filter = new TopicFilterRequest();
        filter.setSubjectId(10L);
        
        BaseFilterSearchRequest<TopicFilterRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(new SearchRequest());

        Page<Topic> page = new PageImpl<>(List.of(existingTopic));
        when(topicRepository.findTopicsBySubjectIdWithSearch(anyLong(), any(), any(Pageable.class))).thenReturn(page);

        var result = topicService.searchTopics(req);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().iterator().next().getTopicName()).isEqualTo("Topic 1");
    }

    @Test
    @DisplayName("TC_QLLH_TOP_11: searchTopics - thÃ nh cÃ´ng khi filters lÃ  null")
    void searchTopics_NullFilters() {
        BaseFilterSearchRequest<TopicFilterRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(null);
        req.setPagination(new SearchRequest());

        when(topicRepository.findTopicsBySubjectIdWithSearch(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existingTopic)));

        var result = topicService.searchTopics(req);

        assertThat(result.getContent()).hasSize(1);
        verify(topicRepository).findTopicsBySubjectIdWithSearch(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_QLLH_TOP_12: searchTopics - nÃ©m lá»—i khi subjectId khÃ´ng tá»“n táº¡i")
    void searchTopics_SubjectNotFound() {
        TopicFilterRequest filter = new TopicFilterRequest();
        filter.setSubjectId(999L);

        BaseFilterSearchRequest<TopicFilterRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filter);
        req.setPagination(new SearchRequest());

        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(999L)).thenReturn(false);

        assertThatThrownBy(() -> topicService.searchTopics(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", AppConst.MessageConst.NOT_FOUND);
    }
}
