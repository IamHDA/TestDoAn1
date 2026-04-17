package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.subject.SubjectCreateRequest;
import com.vn.backend.dto.request.subject.UpdateSubjectRequest;
import com.vn.backend.entities.Subject;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.SubjectRepository;
import com.vn.backend.services.impl.SubjectServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectServiceImpl Unit Tests")
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Subject existingSubject;

    @BeforeEach
    void setUp() {
        existingSubject = Subject.builder()
                .subjectId(1L)
                .subjectCode("CS101")
                .subjectName("Introduction to Computer Science")
                .isDeleted(false)
                .build();
    }

    // ===================== createSubject =====================

    @Test
    @DisplayName("createSubject - thành công khi mã môn học chưa tồn tại")
    void createSubject_Success() {
        SubjectCreateRequest request = new SubjectCreateRequest();
        request.setSubjectCode("CS102");
        request.setSubjectName("Data Structures");

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("CS102")).thenReturn(false);

        subjectService.createSubject(request);

        ArgumentCaptor<Subject> subjectCaptor = ArgumentCaptor.forClass(Subject.class);
        verify(subjectRepository).save(subjectCaptor.capture());
        Subject saved = subjectCaptor.getValue();
        assertThat(saved.getSubjectCode()).isEqualTo("CS102");
        assertThat(saved.getSubjectName()).isEqualTo("Data Structures");
    }

    @Test
    @DisplayName("createSubject - ném exception khi mã môn học đã tồn tại")
    void createSubject_ThrowsException_WhenSubjectCodeAlreadyExists() {
        SubjectCreateRequest request = new SubjectCreateRequest();
        request.setSubjectCode("CS101");
        request.setSubjectName("Duplicate Subject");

        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("CS101")).thenReturn(true);
        when(messageUtils.getMessage(AppConst.MessageConst.SUBJECT_CODE_ALREADY_EXISTS))
                .thenReturn("Subject code already exists");

        assertThatThrownBy(() -> subjectService.createSubject(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.SUBJECT_CODE_ALREADY_EXISTS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(subjectRepository, never()).save(any());
    }

    // ===================== updateSubject =====================

    @Test
    @DisplayName("updateSubject - thành công khi cập nhật tên môn học")
    void updateSubject_Success_UpdateName() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectCode("CS101")
                .subjectName("Updated Name")
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));

        subjectService.updateSubject(1L, request);

        assertThat(existingSubject.getSubjectName()).isEqualTo("Updated Name");
        verify(subjectRepository).save(existingSubject);
    }

    @Test
    @DisplayName("updateSubject - thành công khi đổi mã môn học sang mã chưa tồn tại")
    void updateSubject_Success_ChangeSubjectCode() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectCode("CS999")
                .subjectName("New Name")
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("CS999")).thenReturn(false);

        subjectService.updateSubject(1L, request);

        assertThat(existingSubject.getSubjectCode()).isEqualTo("CS999");
        verify(subjectRepository).save(existingSubject);
    }

    @Test
    @DisplayName("updateSubject - ném exception khi mã môn học mới đã tồn tại")
    void updateSubject_ThrowsException_WhenNewCodeAlreadyExists() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectCode("CS200")
                .subjectName("Test Subject")
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));
        when(subjectRepository.existsBySubjectCodeAndIsDeletedIsFalse("CS200")).thenReturn(true);
        when(messageUtils.getMessage(AppConst.MessageConst.SUBJECT_CODE_ALREADY_EXISTS))
                .thenReturn("Subject code already exists");

        assertThatThrownBy(() -> subjectService.updateSubject(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.SUBJECT_CODE_ALREADY_EXISTS);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("updateSubject - ném exception khi môn học không tồn tại")
    void updateSubject_ThrowsException_WhenSubjectNotFound() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectCode("CS101")
                .subjectName("Test")
                .build();

        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> subjectService.updateSubject(99L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("updateSubject - giữ nguyên mã khi request code giống mã cũ")
    void updateSubject_NoCodeChange_WhenSameCode() {
        UpdateSubjectRequest request = UpdateSubjectRequest.builder()
                .subjectCode("CS101") // same code
                .subjectName("Different Name")
                .build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));

        subjectService.updateSubject(1L, request);

        // code không đổi => không gọi existsBySubjectCode
        verify(subjectRepository, never()).existsBySubjectCodeAndIsDeletedIsFalse(anyString());
        assertThat(existingSubject.getSubjectName()).isEqualTo("Different Name");
        verify(subjectRepository).save(existingSubject);
    }

    // ===================== deleteSubject =====================

    @Test
    @DisplayName("deleteSubject - thành công khi môn học tồn tại")
    void deleteSubject_Success() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));

        subjectService.deleteSubject(1L);

        assertThat(existingSubject.getIsDeleted()).isTrue();
        verify(subjectRepository).save(existingSubject);
    }

    @Test
    @DisplayName("deleteSubject - ném exception khi môn học không tồn tại")
    void deleteSubject_ThrowsException_WhenSubjectNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> subjectService.deleteSubject(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(subjectRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteSubject - đặt isDeleted = true (soft delete)")
    void deleteSubject_SetsIsDeletedTrue() {
        existingSubject.setIsDeleted(false);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existingSubject));

        subjectService.deleteSubject(1L);

        assertThat(existingSubject.getIsDeleted()).isTrue();
    }
}
