package com.vn.backend;

import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.attachment.AttachmentCreateRequestDTO;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.dto.request.submission.SubmissionGradeUpdateRequest;
import com.vn.backend.dto.request.submission.SubmissionSearchRequest;
import com.vn.backend.dto.request.submission.SubmissionUpdateRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.submission.SubmissionDetailResponse;
import com.vn.backend.dto.response.submission.SubmissionSearchResponse;
import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.Attachment;
import com.vn.backend.entities.Submission;
import com.vn.backend.entities.User;
import com.vn.backend.enums.AttachmentType;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AssignmentRepository;
import com.vn.backend.repositories.AttachmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.SubmissionRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.FileService;
import com.vn.backend.services.impl.SubmissionServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionServiceImpl Unit Tests")
class SubmissionServiceImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private FileService fileService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private User studentUser;
    private User teacherUser;
    private Assignment activeAssignment;

    @BeforeEach
    void setUp() {
        studentUser = User.builder().id(1L).build();
        teacherUser = User.builder().id(2L).build();

        activeAssignment = Assignment.builder()
                .assignmentId(10L)
                .classroomId(100L)
                .dueDate(LocalDateTime.now().plusDays(5)) // Not closed, not late
                .submissionClosed(false)
                .build();
    }

    // ===================== getDetailSubmission =====================

    @Test
    @DisplayName("getDetailSubmission - thành công lấy thông tin bài nộp khi có quyền")
    void getDetailSubmission_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser); // Teacher
        when(submissionRepository.hasPermission(1000L, 2L)).thenReturn(true);
        
        Submission submission = Submission.builder()
                .submissionId(1000L)
                .student(studentUser)
                .assignmentId(10L)
                .submissionStatus(SubmissionStatus.NOT_SUBMITTED)
                .gradingStatus(GradingStatus.NOT_GRADED)
                .build();
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(submission));
        when(attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(1000L, AttachmentType.SUBMISSION))
                .thenReturn(new ArrayList<>());

        SubmissionDetailResponse response = submissionService.getDetailSubmission("1000");

        assertThat(response.getSubmissionId()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("getDetailSubmission - bị cấm khi không có quyền")
    void getDetailSubmission_ThrowsForbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // Another student
        when(submissionRepository.hasPermission(1000L, 1L)).thenReturn(false);
        when(messageUtils.getMessage(MessageConst.FORBIDDEN)).thenReturn("Forbidden");

        assertThatThrownBy(() -> submissionService.getDetailSubmission("1000"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Forbidden");
    }

    // ===================== createDefaultSubmissions =====================

    @Test
    @DisplayName("createDefaultSubmissions - thành công khởi tạo submission cho các active student")
    void createDefaultSubmissions_Success() {
        when(classMemberRepository.getClassMemberIdsActive(100L, ClassMemberRole.STUDENT))
                .thenReturn(Set.of(1L, 2L, 3L)); // 3 students
                
        submissionService.createDefaultSubmissions(activeAssignment);

        verify(submissionRepository, times(3)).save(any(Submission.class));
    }

    // ===================== addAttachmentToSubmission =====================

    @Test
    @DisplayName("addAttachmentToSubmission - nộp bài đúng hạn")
    void addAttachmentToSubmission_Success_NotLate() {
        when(authService.getCurrentUser()).thenReturn(studentUser);

        Submission submission = Submission.builder().submissionId(1000L).assignmentId(10L).studentId(1L).build();
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(submission));

        when(assignmentRepository.findAssignmentIfUserCanSubmit(1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(activeAssignment)); // dueDate = now + 5 days -> Not late

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        com.vn.backend.dto.request.attachment.AttachmentCreateRequest attachmentReq = com.vn.backend.dto.request.attachment.AttachmentCreateRequest.builder()
                .fileName("BTVN.pdf")
                .fileUrl("URL")
                .build();
        request.setAttachmentCreateRequestList(List.of(attachmentReq));

        submissionService.addAttachmentToSubmission("1000", request);

        verify(attachmentRepository).save(any(Attachment.class));
        verify(submissionRepository).save(submission);
        assertThat(submission.getSubmissionStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("addAttachmentToSubmission - ném lỗi khi nộp bài trễ hạn và submission bị đóng")
    void addAttachmentToSubmission_ThrowsWhenLateAndClosed() {
        when(authService.getCurrentUser()).thenReturn(studentUser);

        Submission submission = Submission.builder().submissionId(1000L).assignmentId(10L).studentId(1L).build();
        when(submissionRepository.findBySubmissionIdAndStudentId(1000L, 1L)).thenReturn(Optional.of(submission));

        Assignment closedAssignment = Assignment.builder()
                .assignmentId(10L)
                .dueDate(LocalDateTime.now().minusDays(1)) // Late
                .submissionClosed(true) // Closed
                .build();
        when(assignmentRepository.findAssignmentIfUserCanSubmit(1L, 10L, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE))
                .thenReturn(Optional.of(closedAssignment));

        when(messageUtils.getMessage(MessageConst.LATE_SUBMISSION_NOT_ALLOWED)).thenReturn("Late disabled");

        SubmissionUpdateRequest request = new SubmissionUpdateRequest();
        request.setAttachmentCreateRequestList(new ArrayList<>());
        
        assertThatThrownBy(() -> submissionService.addAttachmentToSubmission("1000", request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Late disabled");
    }

    // ===================== markSubmission =====================

    @Test
    @DisplayName("markSubmission - giáo viên chấm điểm thành công")
    void markSubmission_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(submissionRepository.hasPermission(1000L, 2L)).thenReturn(true);
        
        Submission submission = Submission.builder().submissionId(1000L).build();
        when(submissionRepository.findById(1000L)).thenReturn(Optional.of(submission));

        SubmissionGradeUpdateRequest request = new SubmissionGradeUpdateRequest();
        request.setGrade("9.5");

        submissionService.markSubmission("1000", request);

        verify(submissionRepository).save(submission);
        assertThat(submission.getGrade()).isEqualTo(9.5);
        assertThat(submission.getGradingStatus()).isEqualTo(GradingStatus.GRADED);
    }

}
