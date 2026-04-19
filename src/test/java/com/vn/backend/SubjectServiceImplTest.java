package com.vn.backend;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.subject.*;
import com.vn.backend.entities.Subject;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.impl.SubjectServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static com.vn.backend.constants.AppConst.MessageConst;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubjectServiceImpl Unit Tests")
class SubjectServiceImplTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Subject existingSubject;

    @BeforeEach
    void setUp() {
        existingSubject = Subject.builder()
                .subjectId(1L)
                .subjectCode("MATH101")
                .subjectName("Toán cơ bản")
                .isDeleted(false)
                .build();
        
        when(messageUtils.getMessage(anyString())).thenReturn("Error message");
    }

    // ================== createSubject ==================
    @Test
    @DisplayName("TC_QLLH_SUB_01: createSubject - ném exception khi mã môn đã tồn tại")
    void createSubject_AlreadyExists() {
        SubjectCreateRequest request = mock(SubjectCreateRequest.class);
        SubjectCreateRequestDTO dto = SubjectCreateRequestDTO.builder().subjectCode("MATH101").build();
        when(request.toDTO()).thenReturn(dto);
        
        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("MATH101")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.createSubject(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Error message");
    }

    @Test
    @DisplayName("TC_QLLH_SUB_02: createSubject - thành công")
    void createSubject_Success() {
        SubjectCreateRequest request = mock(SubjectCreateRequest.class);
        SubjectCreateRequestDTO dto = SubjectCreateRequestDTO.builder()
                .subjectCode("NEW_SUB")
                .subjectName("New Subject")
                .build();
        when(request.toDTO()).thenReturn(dto);

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("NEW_SUB")).thenReturn(false);

        subjectService.createSubject(request);

        verify(subjectRepository).save(any(Subject.class));
    }

    // ================== searchSubject ==================
    @Test
    @DisplayName("TC_QLLH_SUB_03: searchSubject - thành công")
    void searchSubject_Success() {
        SubjectSearchRequest filters = mock(SubjectSearchRequest.class);
        when(filters.toDTO()).thenReturn(SubjectSearchRequestDTO.builder().build());

        BaseFilterSearchRequest<SubjectSearchRequest> req = new BaseFilterSearchRequest<>();
        req.setFilters(filters);
        req.setPagination(new SearchRequest());

        Page<Subject> page = new PageImpl<>(List.of(existingSubject));
        when(subjectRepository.searchSubject(any(), any(Pageable.class))).thenReturn(page);

        var result = subjectService.searchSubject(req);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }

    // ================== updateSubject ==================
    @Test
    @DisplayName("TC_QLLH_SUB_04: updateSubject - ném exception khi không tìm thấy môn học")
    void updateSubject_NotFound() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateSubjectRequest request = new UpdateSubjectRequest();

        assertThatThrownBy(() -> subjectService.updateSubject(999L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("TC_QLLH_SUB_05: updateSubject - ném exception khi đổi mã sang một mã đã tồn tại")
    void updateSubject_DuplicateCode() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));
        
        UpdateSubjectRequest request = new UpdateSubjectRequest();
        request.setSubjectCode("PHYS101"); // Mã mới

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("PHYS101")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.updateSubject(1L, request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_SUB_06: updateSubject - cập nhật tên môn thành công")
    void updateSubject_NameSuccess() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));
        
        UpdateSubjectRequest request = new UpdateSubjectRequest();
        request.setSubjectName("Toán cao cấp");

        subjectService.updateSubject(1L, request);

        assertThat(existingSubject.getSubjectName()).isEqualTo("Toán cao cấp");
        verify(subjectRepository).save(existingSubject);
    }

    @Test
    @DisplayName("TC_QLLH_SUB_07: updateSubject - cập nhật cả mã và tên thành công")
    void updateSubject_AllSuccess() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));
        
        UpdateSubjectRequest request = new UpdateSubjectRequest();
        request.setSubjectCode("MATH202");
        request.setSubjectName("Toán 2");

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("MATH202")).thenReturn(false);

        subjectService.updateSubject(1L, request);

        assertThat(existingSubject.getSubjectCode()).isEqualTo("MATH202");
        assertThat(existingSubject.getSubjectName()).isEqualTo("Toán 2");
        verify(subjectRepository).save(existingSubject);
    }

    // ================== deleteSubject ==================
    @Test
    @DisplayName("TC_QLLH_SUB_08: deleteSubject - ném exception khi không tìm thấy")
    void deleteSubject_NotFound() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subjectService.deleteSubject(999L)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_SUB_09: deleteSubject - xóa mềm thành công")
    void deleteSubject_Success() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));

        subjectService.deleteSubject(1L);

        assertThat(existingSubject.getIsDeleted()).isTrue();
        verify(subjectRepository).save(existingSubject);
    }
}
