package com.vn.backend.unit;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.subject.*;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.subject.SubjectSearchResponse;
import com.vn.backend.entities.Subject;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.impl.SubjectServiceImpl;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubjectServiceImplTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private SubjectRepository subjectRepository;

    private SubjectServiceImpl service;

    private final Map<Long, Subject> subjectStore = new HashMap<>();
    private Subject savedSubject;

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new SubjectServiceImpl(messageUtils, subjectRepository);

        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> {
            savedSubject = invocation.getArgument(0);

            if (savedSubject.getSubjectId() == null) {
                savedSubject.setSubjectId((long) (subjectStore.size() + 1));
            }

            subjectStore.put(savedSubject.getSubjectId(), savedSubject);
            return savedSubject;
        });

        when(subjectRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long subjectId = invocation.getArgument(0);
            return Optional.ofNullable(subjectStore.get(subjectId));
        });

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse(anyString()))
                .thenAnswer(invocation -> {
                    String subjectCode = invocation.getArgument(0);

                    return subjectStore.values()
                            .stream()
                            .anyMatch(subject ->
                                    subjectCode.equals(subject.getSubjectCode())
                                            && !Boolean.TRUE.equals(subject.getIsDeleted())
                            );
                });
    }

    private Subject subject(Long subjectId, String subjectCode, String subjectName) {
        return Subject.builder()
                .subjectId(subjectId)
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .isDeleted(false)
                .build();
    }

    private SubjectCreateRequest createRequest(String subjectCode, String subjectName) {
        SubjectCreateRequest request = mock(SubjectCreateRequest.class);

        SubjectCreateRequestDTO dto = SubjectCreateRequestDTO.builder()
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .build();

        when(request.toDTO()).thenReturn(dto);

        return request;
    }

    private UpdateSubjectRequest updateRequest(String subjectCode, String subjectName) {
        UpdateSubjectRequest request = new UpdateSubjectRequest();
        request.setSubjectCode(subjectCode);
        request.setSubjectName(subjectName);
        return request;
    }

    private SearchRequest pagination() {
        SearchRequest pagination = new SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        return pagination;
    }

    private BaseFilterSearchRequest<SubjectSearchRequest> searchRequest() {
        BaseFilterSearchRequest<SubjectSearchRequest> request =
                mock(BaseFilterSearchRequest.class);

        SubjectSearchRequest filters = mock(SubjectSearchRequest.class);

        SubjectSearchRequestDTO dto = SubjectSearchRequestDTO.builder()
                .subjectName("java")
                .build();

        when(request.getFilters()).thenReturn(filters);
        when(filters.toDTO()).thenReturn(dto);
        when(request.getPagination()).thenReturn(pagination());

        return request;
    }

    @Nested
    class CreateSubjectTests {

        @Test
        void createSubject_Success() {
            SubjectCreateRequest request = createRequest("JAVA101", "Java Programming");

            service.createSubject(request);

            assertNotNull(savedSubject);
            assertEquals("JAVA101", savedSubject.getSubjectCode());
            assertEquals("Java Programming", savedSubject.getSubjectName());

            verify(subjectRepository).save(any(Subject.class));
        }

        @Test
        void createSubject_Fail_ThrowsWhenSubjectCodeExists() {
            subjectStore.put(
                    SUBJECT_ID,
                    subject(SUBJECT_ID, "JAVA101", "Old Java")
            );

            SubjectCreateRequest request = createRequest("JAVA101", "Java Programming");

            assertThrows(AppException.class, () -> service.createSubject(request));

            verify(subjectRepository, never()).save(any(Subject.class));
        }

        @Test
        void createSubject_Fail_ThrowsWhenSubjectNameTooLong() {
            SubjectCreateRequest request = createRequest("JAVA101", "A".repeat(101));

            assertThrows(AppException.class, () -> service.createSubject(request));

            verify(subjectRepository, never()).save(any(Subject.class));
        }
    }

    @Nested
    class SearchSubjectTests {

        @Test
        void searchSubject_Success() {
            Subject subject = subject(SUBJECT_ID, "JAVA101", "Java Programming");

            when(subjectRepository.searchSubject(
                    any(SubjectSearchRequestDTO.class),
                    any(Pageable.class)
            )).thenReturn(new PageImpl<>(List.of(subject)));

            ResponseListData<SubjectSearchResponse> result =
                    service.searchSubject(searchRequest());

            assertNotNull(result);

            verify(subjectRepository).searchSubject(
                    any(SubjectSearchRequestDTO.class),
                    any(Pageable.class)
            );
        }
    }

    @Nested
    class UpdateSubjectTests {

        @Test
        void updateSubject_Success_UpdatesCodeAndName() {
            Subject existing = subject(SUBJECT_ID, "OLD101", "Old Subject");
            subjectStore.put(SUBJECT_ID, existing);

            service.updateSubject(
                    SUBJECT_ID,
                    updateRequest("JAVA101", "Java Programming")
            );

            assertNotNull(savedSubject);
            assertEquals("JAVA101", savedSubject.getSubjectCode());
            assertEquals("Java Programming", savedSubject.getSubjectName());

            verify(subjectRepository).save(existing);
        }

        @Test
        void updateSubject_Success_OnlyUpdatesNameWhenCodeSame() {
            Subject existing = subject(SUBJECT_ID, "JAVA101", "Old Subject");
            subjectStore.put(SUBJECT_ID, existing);

            service.updateSubject(
                    SUBJECT_ID,
                    updateRequest("JAVA101", "New Subject Name")
            );

            assertEquals("JAVA101", savedSubject.getSubjectCode());
            assertEquals("New Subject Name", savedSubject.getSubjectName());

            verify(subjectRepository).save(existing);
        }

        @Test
        void updateSubject_Success_DoesNotChangeNullFields() {
            Subject existing = subject(SUBJECT_ID, "JAVA101", "Old Subject");
            subjectStore.put(SUBJECT_ID, existing);

            service.updateSubject(
                    SUBJECT_ID,
                    updateRequest(null, null)
            );

            assertEquals("JAVA101", savedSubject.getSubjectCode());
            assertEquals("Old Subject", savedSubject.getSubjectName());

            verify(subjectRepository).save(existing);
        }

        @Test
        void updateSubject_Fail_ThrowsWhenSubjectMissing() {
            assertThrows(AppException.class, () ->
                    service.updateSubject(
                            99L,
                            updateRequest("JAVA101", "Java Programming")
                    )
            );

            verify(subjectRepository, never()).save(any(Subject.class));
        }

        @Test
        void updateSubject_Fail_ThrowsWhenNewCodeExists() {
            Subject current = subject(SUBJECT_ID, "OLD101", "Old Subject");
            Subject other = subject(2L, "JAVA101", "Java Programming");

            subjectStore.put(SUBJECT_ID, current);
            subjectStore.put(2L, other);

            assertThrows(AppException.class, () ->
                    service.updateSubject(
                            SUBJECT_ID,
                            updateRequest("JAVA101", "New Name")
                    )
            );

            verify(subjectRepository, never()).save(current);
        }

        @Test
        void updateSubject_Fail_ThrowsWhenSubjectNameTooLong() {
            Subject existing = subject(SUBJECT_ID, "JAVA101", "Old Subject");
            subjectStore.put(SUBJECT_ID, existing);

            assertThrows(AppException.class, () ->
                    service.updateSubject(
                            SUBJECT_ID,
                            updateRequest(null, "A".repeat(101))
                    )
            );

            verify(subjectRepository, never()).save(existing);
        }
    }

    @Nested
    class DeleteSubjectTests {

        @Test
        void deleteSubject_Success_SoftDeletesSubject() {
            Subject existing = subject(SUBJECT_ID, "JAVA101", "Java Programming");
            subjectStore.put(SUBJECT_ID, existing);

            service.deleteSubject(SUBJECT_ID);

            assertNotNull(savedSubject);
            assertTrue(savedSubject.getIsDeleted());

            verify(subjectRepository).save(existing);
        }

        @Test
        void deleteSubject_Fail_ThrowsWhenSubjectMissing() {
            assertThrows(AppException.class, () -> service.deleteSubject(99L));

            verify(subjectRepository, never()).save(any(Subject.class));
        }
    }
}