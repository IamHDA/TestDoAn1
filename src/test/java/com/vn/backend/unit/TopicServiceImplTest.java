package com.vn.backend.unit;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.topic.CreateApprovalTopicRequest;
import com.vn.backend.dto.request.topic.CreateTopicRequest;
import com.vn.backend.dto.request.topic.TopicFilterRequest;
import com.vn.backend.dto.request.topic.UpdateTopicRequest;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.topic.TopicResponse;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TopicServiceImplTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TEACHER_ID = 4L;
    private static final Long STUDENT_ID = 8L;
    private static final Long SUBJECT_ID = 10L;
    private static final Long TOPIC_ID = 20L;
    private static final Long PREREQUISITE_TOPIC_ID = 30L;

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

    private TopicServiceImpl service;

    private final Map<Long, Topic> topicStore = new HashMap<>();
    private List<Topic> savedTopics = new ArrayList<>();
    private Topic savedTopic;
    private long topicSequence = 100L;

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new TopicServiceImpl(
                messageUtils,
                topicRepository,
                subjectRepository,
                approvalRequestItemsRepository,
                authService,
                approvalRequestService
        );

        mockTopicRepositoryStorage();
    }

    private void mockTopicRepositoryStorage() {
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            savedTopic = invocation.getArgument(0);

            if (savedTopic.getTopicId() == null) {
                savedTopic.setTopicId(topicSequence++);
            }

            topicStore.put(savedTopic.getTopicId(), savedTopic);
            return savedTopic;
        });

        when(topicRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<Topic> topics = invocation.getArgument(0);
            savedTopics = new ArrayList<>();

            for (Topic topic : topics) {
                if (topic.getTopicId() == null) {
                    topic.setTopicId(topicSequence++);
                }

                topicStore.put(topic.getTopicId(), topic);
                savedTopics.add(topic);
            }

            return savedTopics;
        });

        when(topicRepository.findByTopicIdAndIsDeleted(anyLong(), anyBoolean()))
                .thenAnswer(invocation -> {
                    Long topicId = invocation.getArgument(0);
                    Boolean isDeleted = invocation.getArgument(1);

                    Topic topic = topicStore.get(topicId);
                    if (topic == null) {
                        return Optional.empty();
                    }

                    if (!isDeleted.equals(topic.getIsDeleted())) {
                        return Optional.empty();
                    }

                    return Optional.of(topic);
                });

        when(topicRepository.findAllById(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);

                    return ids.stream()
                            .map(topicStore::get)
                            .filter(topic -> topic != null)
                            .toList();
                });
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .role(role)
                .fullName("User " + id)
                .build();
    }

    private void mockCurrentUser(Long userId, Role role) {
        when(authService.getCurrentUser()).thenReturn(user(userId, role));
    }

    private void mockSubjectExists(Long subjectId, boolean exists) {
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(subjectId))
                .thenReturn(exists);
    }

    private Topic topic(Long topicId, Long subjectId, String topicName, boolean active, boolean deleted) {
        return Topic.builder()
                .topicId(topicId)
                .subjectId(subjectId)
                .topicName(topicName)
                .createdBy(TEACHER_ID)
                .isActive(active)
                .isDeleted(deleted)
                .build();
    }

    private CreateTopicRequest createTopicRequest(Long topicId, String topicName, Long prerequisiteTopicId) {
        CreateTopicRequest request = new CreateTopicRequest();
        request.setTopicId(topicId);
        request.setTopicName(topicName);
        request.setPrerequisiteTopicId(prerequisiteTopicId);
        return request;
    }

    private CreateApprovalTopicRequest approvalTopicRequest(List<CreateTopicRequest> topicRequests) {
        CreateApprovalTopicRequest request = new CreateApprovalTopicRequest();
        request.setSubjectId(SUBJECT_ID);
        request.setRequestType(RequestType.TOPIC_CREATE);
        request.setRequestDescription("Need approval for new topics");
        request.setTopicRequests(topicRequests);
        return request;
    }

    private UpdateTopicRequest updateTopicRequest(String topicName, Long prerequisiteTopicId) {
        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setTopicName(topicName);
        request.setPrerequisiteTopicId(prerequisiteTopicId);
        return request;
    }

    private BaseFilterSearchRequest<TopicFilterRequest> searchRequest(Long subjectId, String keyword) {
        BaseFilterSearchRequest<TopicFilterRequest> request = new BaseFilterSearchRequest<>();

        TopicFilterRequest filters = new TopicFilterRequest();
        filters.setSubjectId(subjectId);
        filters.setKeyword(keyword);

        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");

        request.setFilters(filters);
        request.setPagination(pagination);

        return request;
    }

    @Nested
    class ApprovalRequestTopicTests {

        @Test
        void approvalRequestTopic_Success_CreatesInactiveTopicsAndApprovalRequest() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    EntityType.TOPIC
            )).thenReturn(List.of());

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null),
                    createTopicRequest(null, "Java OOP", null)
            ));

            service.approvalRequestTopic(request);

            assertEquals(2, savedTopics.size());

            Topic firstTopic = savedTopics.get(0);
            Topic secondTopic = savedTopics.get(1);

            assertEquals("Java Basic", firstTopic.getTopicName());
            assertEquals(SUBJECT_ID, firstTopic.getSubjectId());
            assertEquals(TEACHER_ID, firstTopic.getCreatedBy());
            assertFalse(firstTopic.getIsDeleted());
            assertFalse(firstTopic.getIsActive());

            assertEquals("Java OOP", secondTopic.getTopicName());
            assertEquals(SUBJECT_ID, secondTopic.getSubjectId());
            assertEquals(TEACHER_ID, secondTopic.getCreatedBy());
            assertFalse(secondTopic.getIsDeleted());
            assertFalse(secondTopic.getIsActive());

            verify(topicRepository).saveAllAndFlush(anyList());

            verify(approvalRequestService).createRequest(
                    eq(RequestType.TOPIC_CREATE),
                    eq("Need approval for new topics"),
                    eq(TEACHER_ID),
                    eq(savedTopics.stream().map(Topic::getTopicId).toList())
            );
        }

        @Test
        void approvalRequestTopic_Fail_ThrowsWhenCurrentUserIsNotTeacher() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null)
            ));

            assertThrows(AppException.class, () -> service.approvalRequestTopic(request));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
            verify(approvalRequestService, never()).createRequest(any(), any(), anyLong(), anyList());
        }

        @Test
        void approvalRequestTopic_Fail_ThrowsWhenSubjectMissing() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, false);

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null)
            ));

            assertThrows(AppException.class, () -> service.approvalRequestTopic(request));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
            verify(approvalRequestService, never()).createRequest(any(), any(), anyLong(), anyList());
        }

        @Test
        void approvalRequestTopic_Fail_ThrowsWhenTopicRequestsNull() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            CreateApprovalTopicRequest request = approvalTopicRequest(null);

            assertThrows(AppException.class, () -> service.approvalRequestTopic(request));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
            verify(approvalRequestService, never()).createRequest(any(), any(), anyLong(), anyList());
        }

        @Test
        void approvalRequestTopic_Fail_ThrowsWhenTopicRequestsEmpty() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of());

            assertThrows(AppException.class, () -> service.approvalRequestTopic(request));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
            verify(approvalRequestService, never()).createRequest(any(), any(), anyLong(), anyList());
        }

        @Test
        void approvalRequestTopic_Fail_ThrowsWhenPendingRequestExistsInSameSubject() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            Topic pendingTopic = topic(TOPIC_ID, SUBJECT_ID, "Pending Topic", false, false);
            topicStore.put(TOPIC_ID, pendingTopic);

            when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    EntityType.TOPIC
            )).thenReturn(List.of(TOPIC_ID));

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null)
            ));

            assertThrows(AppException.class, () -> service.approvalRequestTopic(request));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
            verify(approvalRequestService, never()).createRequest(any(), any(), anyLong(), anyList());
        }

        @Test
        void approvalRequestTopic_Success_IgnoresPendingDeletedTopicInSameSubject() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            Topic deletedPendingTopic = topic(TOPIC_ID, SUBJECT_ID, "Deleted Pending Topic", false, true);
            topicStore.put(TOPIC_ID, deletedPendingTopic);

            when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    EntityType.TOPIC
            )).thenReturn(List.of(TOPIC_ID));

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null)
            ));

            service.approvalRequestTopic(request);

            assertEquals(1, savedTopics.size());
            verify(approvalRequestService).createRequest(
                    eq(RequestType.TOPIC_CREATE),
                    eq("Need approval for new topics"),
                    eq(TEACHER_ID),
                    anyList()
            );
        }

        @Test
        void approvalRequestTopic_Success_AllowsPendingTopicInDifferentSubject() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockSubjectExists(SUBJECT_ID, true);

            Topic pendingTopicDifferentSubject = topic(TOPIC_ID, 999L, "Other Subject Topic", false, false);
            topicStore.put(TOPIC_ID, pendingTopicDifferentSubject);

            when(approvalRequestItemsRepository.findEntityIdsByPendingRequest(
                    RequestType.TOPIC_CREATE,
                    ApprovalStatus.PENDING,
                    EntityType.TOPIC
            )).thenReturn(List.of(TOPIC_ID));

            CreateApprovalTopicRequest request = approvalTopicRequest(List.of(
                    createTopicRequest(null, "Java Basic", null)
            ));

            service.approvalRequestTopic(request);

            assertEquals(1, savedTopics.size());
            verify(approvalRequestService).createRequest(
                    eq(RequestType.TOPIC_CREATE),
                    eq("Need approval for new topics"),
                    eq(TEACHER_ID),
                    anyList()
            );
        }
    }

    @Nested
    class SaveAllTopicTests {

        @Test
        void saveAllTopic_Success_CreatesNewTopics() {
            User teacher = user(TEACHER_ID, Role.TEACHER);

            List<CreateTopicRequest> requests = List.of(
                    createTopicRequest(null, "  Java Basic  ", null),
                    createTopicRequest(null, "Java OOP", TOPIC_ID)
            );

            List<Topic> result = service.saveAllTopic(requests, SUBJECT_ID, teacher);

            assertEquals(2, result.size());

            Topic firstTopic = result.get(0);
            Topic secondTopic = result.get(1);

            assertEquals("Java Basic", firstTopic.getTopicName());
            assertEquals(SUBJECT_ID, firstTopic.getSubjectId());
            assertEquals(TEACHER_ID, firstTopic.getCreatedBy());
            assertFalse(firstTopic.getIsDeleted());
            assertFalse(firstTopic.getIsActive());

            assertEquals("Java OOP", secondTopic.getTopicName());
            assertEquals(TOPIC_ID, secondTopic.getPrerequisiteTopicId());

            assertEquals(firstTopic, topicRepository.findByTopicIdAndIsDeleted(firstTopic.getTopicId(), false).orElseThrow());
            assertEquals(secondTopic, topicRepository.findByTopicIdAndIsDeleted(secondTopic.getTopicId(), false).orElseThrow());
        }

        @Test
        void saveAllTopic_Success_UsesExistingTopicWhenTopicIdProvided() {
            User teacher = user(TEACHER_ID, Role.TEACHER);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Existing Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            List<CreateTopicRequest> requests = List.of(
                    createTopicRequest(TOPIC_ID, "Ignored Name", null)
            );

            List<Topic> result = service.saveAllTopic(requests, SUBJECT_ID, teacher);

            assertEquals(1, result.size());
            assertEquals(existing, result.get(0));
            assertEquals(existing, topicRepository.findByTopicIdAndIsDeleted(TOPIC_ID, false).orElseThrow());
        }

        @Test
        void saveAllTopic_Fail_ThrowsWhenTopicNameNull() {
            User teacher = user(TEACHER_ID, Role.TEACHER);

            List<CreateTopicRequest> requests = List.of(
                    createTopicRequest(null, null, null)
            );

            assertThrows(AppException.class, () -> service.saveAllTopic(requests, SUBJECT_ID, teacher));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
        }

        @Test
        void saveAllTopic_Fail_ThrowsWhenTopicNameBlank() {
            User teacher = user(TEACHER_ID, Role.TEACHER);

            List<CreateTopicRequest> requests = List.of(
                    createTopicRequest(null, "   ", null)
            );

            assertThrows(AppException.class, () -> service.saveAllTopic(requests, SUBJECT_ID, teacher));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
        }

        @Test
        void saveAllTopic_Fail_ThrowsWhenExistingTopicMissing() {
            User teacher = user(TEACHER_ID, Role.TEACHER);

            List<CreateTopicRequest> requests = List.of(
                    createTopicRequest(999L, "Existing Topic", null)
            );

            assertThrows(AppException.class, () -> service.saveAllTopic(requests, SUBJECT_ID, teacher));

            verify(topicRepository, never()).saveAllAndFlush(anyList());
        }
    }

    @Nested
    class UpdateTopicTests {

        @Test
        void updateTopic_Success_UpdatesTopicName() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Old Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            service.updateTopic(TOPIC_ID, updateTopicRequest("New Topic", null));

            assertNotNull(savedTopic);
            assertEquals("New Topic", savedTopic.getTopicName());
            assertEquals(existing, topicRepository.findByTopicIdAndIsDeleted(TOPIC_ID, false).orElseThrow());

            verify(topicRepository).save(existing);
        }

        @Test
        void updateTopic_Success_UpdatesPrerequisiteTopic() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Topic", true, false);
            Topic prerequisite = topic(PREREQUISITE_TOPIC_ID, SUBJECT_ID, "Prerequisite", true, false);

            topicStore.put(TOPIC_ID, existing);
            topicStore.put(PREREQUISITE_TOPIC_ID, prerequisite);

            service.updateTopic(TOPIC_ID, updateTopicRequest("Topic Updated", PREREQUISITE_TOPIC_ID));

            assertEquals("Topic Updated", savedTopic.getTopicName());
            assertEquals(PREREQUISITE_TOPIC_ID, savedTopic.getPrerequisiteTopicId());
            assertEquals(existing, topicRepository.findByTopicIdAndIsDeleted(TOPIC_ID, false).orElseThrow());

            verify(topicRepository).save(existing);
        }

        @Test
        void updateTopic_Success_DoesNotChangeNameWhenNameIsNull() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Old Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            service.updateTopic(TOPIC_ID, updateTopicRequest(null, null));

            assertEquals("Old Topic", savedTopic.getTopicName());
            assertEquals(existing, topicRepository.findByTopicIdAndIsDeleted(TOPIC_ID, false).orElseThrow());

            verify(topicRepository).save(existing);
        }

        @Test
        void updateTopic_Fail_ThrowsWhenTopicMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            assertThrows(AppException.class, () ->
                    service.updateTopic(999L, updateTopicRequest("New Topic", null))
            );

            verify(topicRepository, never()).save(any(Topic.class));
        }

        @Test
        void updateTopic_Fail_ThrowsWhenCurrentUserIsNotAdmin() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Old Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            assertThrows(AppException.class, () ->
                    service.updateTopic(TOPIC_ID, updateTopicRequest("New Topic", null))
            );

            verify(topicRepository, never()).save(any(Topic.class));
        }

        @Test
        void updateTopic_Fail_ThrowsWhenPrerequisiteIsItself() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Old Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            assertThrows(AppException.class, () ->
                    service.updateTopic(TOPIC_ID, updateTopicRequest("New Topic", TOPIC_ID))
            );

            verify(topicRepository, never()).save(any(Topic.class));
        }

        @Test
        void updateTopic_Fail_ThrowsWhenPrerequisiteTopicMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Old Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            assertThrows(AppException.class, () ->
                    service.updateTopic(TOPIC_ID, updateTopicRequest("New Topic", PREREQUISITE_TOPIC_ID))
            );

            verify(topicRepository, never()).save(any(Topic.class));
        }
    }

    @Nested
    class DeleteTopicTests {

        @Test
        void deleteTopic_Success_SoftDeletesTopic() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            service.deleteTopic(TOPIC_ID);

            assertNotNull(savedTopic);
            assertTrue(savedTopic.getIsDeleted());
            assertEquals(existing, topicRepository.findByTopicIdAndIsDeleted(TOPIC_ID, true).orElseThrow());

            verify(topicRepository).save(existing);
        }

        @Test
        void deleteTopic_Fail_ThrowsWhenTopicMissing() {
            mockCurrentUser(ADMIN_ID, Role.ADMIN);

            assertThrows(AppException.class, () -> service.deleteTopic(999L));

            verify(topicRepository, never()).save(any(Topic.class));
        }

        @Test
        void deleteTopic_Fail_ThrowsWhenCurrentUserIsNotAdmin() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);

            Topic existing = topic(TOPIC_ID, SUBJECT_ID, "Topic", true, false);
            topicStore.put(TOPIC_ID, existing);

            assertThrows(AppException.class, () -> service.deleteTopic(TOPIC_ID));

            assertFalse(existing.getIsDeleted());
            verify(topicRepository, never()).save(any(Topic.class));
        }
    }

    @Nested
    class SearchTopicsTests {

        @Test
        void searchTopics_Success_WithSubjectIdAndKeyword() {
            mockSubjectExists(SUBJECT_ID, true);

            Topic first = topic(1L, SUBJECT_ID, "Java Basic", true, false);
            Topic second = topic(2L, SUBJECT_ID, "Java OOP", true, false);

            when(topicRepository.findTopicsBySubjectIdWithSearch(
                    eq(SUBJECT_ID),
                    eq("java"),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(first, second)));

            ResponseListData<TopicResponse> response =
                    service.searchTopics(searchRequest(SUBJECT_ID, "java"));

            assertNotNull(response);
            assertEquals(2, response.getContent().size());

            List<TopicResponse> content = new ArrayList<>(response.getContent());

            assertEquals("Java Basic", content.get(0).getTopicName());
            assertEquals("Java OOP", content.get(1).getTopicName());

            verify(topicRepository).findTopicsBySubjectIdWithSearch(
                    eq(SUBJECT_ID),
                    eq("java"),
                    any(Pageable.class)
            );
        }

        @Test
        void searchTopics_Success_WhenFiltersNull() {
            BaseFilterSearchRequest<TopicFilterRequest> request = new BaseFilterSearchRequest<>();

            SearchRequest pagination = new SearchRequest();
            pagination.setPageNum("1");
            pagination.setPageSize("10");

            request.setFilters(null);
            request.setPagination(pagination);

            Topic first = topic(1L, SUBJECT_ID, "Java Basic", true, false);

            when(topicRepository.findTopicsBySubjectIdWithSearch(
                    eq(null),
                    eq(null),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(first)));

            ResponseListData<TopicResponse> response = service.searchTopics(request);

            assertNotNull(response);
            assertEquals(1, response.getContent().size());

            List<TopicResponse> content = new ArrayList<>(response.getContent());

            assertEquals("Java Basic", content.get(0).getTopicName());

            verify(topicRepository).findTopicsBySubjectIdWithSearch(
                    eq(null),
                    eq(null),
                    any(Pageable.class)
            );
        }

        @Test
        void searchTopics_Fail_ThrowsWhenSubjectMissing() {
            mockSubjectExists(SUBJECT_ID, false);

            assertThrows(AppException.class, () ->
                    service.searchTopics(searchRequest(SUBJECT_ID, "java"))
            );

            verify(topicRepository, never()).findTopicsBySubjectIdWithSearch(any(), any(), any(Pageable.class));
        }
    }
}