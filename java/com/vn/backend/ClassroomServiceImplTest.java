package com.vn.backend;

import com.vn.backend.constants.AppConst;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.ApprovalRequestService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.impl.ClassroomServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassroomServiceImpl Unit Tests")
class ClassroomServiceImplTest {

    @Mock
    private AuthService authService;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private ClassroomSettingRepository classroomSettingRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private ApprovalRequestService approvalRequestService;

    @Mock
    private MessageUtils messageUtils;

    @InjectMocks
    private ClassroomServiceImpl classroomService;

    private User teacherUser;
    private User studentUser;
    private Classroom activeClassroom;
    private ClassMember activeMember;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder()
                .id(1L)
                .username("teacher01")
                .email("teacher@example.com")
                .role(Role.TEACHER)
                .isActive(true)
                .isDeleted(false)
                .build();

        studentUser = User.builder()
                .id(2L)
                .username("student01")
                .email("student@example.com")
                .role(Role.STUDENT)
                .isActive(true)
                .isDeleted(false)
                .build();

        activeClassroom = Classroom.builder()
                .classroomId(100L)
                .classCode("abc123")
                .className("Test Classroom")
                .teacherId(1L)
                .classroomStatus(ClassroomStatus.ACTIVE)
                .classCodeStatus(ClassCodeStatus.ACTIVE)
                .isActive(true)
                .teacher(teacherUser)
                .subject(com.vn.backend.entities.Subject.builder().subjectId(1L).subjectName("Math").subjectCode("MATH101").build())
                .schedules(new java.util.ArrayList<>())
                .build();

        activeMember = ClassMember.builder()
                .memberId(10L)
                .classroomId(100L)
                .userId(2L)
                .memberStatus(ClassMemberStatus.ACTIVE)
                .classroom(activeClassroom)
                .build();
    }

    // ===================== getDetailClassroom =====================

    @Test
    @DisplayName("getDetailClassroom - thành công khi lớp tồn tại")
    void getDetailClassroom_Success() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));

        var result = classroomService.getDetailClassroom("100");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getDetailClassroom - ném exception khi lớp không tồn tại")
    void getDetailClassroom_ThrowsException_WhenClassroomNotFound() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(999L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> classroomService.getDetailClassroom("999"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ===================== getClassroomHeader =====================

    @Test
    @DisplayName("getClassroomHeader - thành công khi lớp tồn tại")
    void getClassroomHeader_Success() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));

        var result = classroomService.getClassroomHeader("100");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getClassroomHeader - ném exception khi lớp không tồn tại")
    void getClassroomHeader_ThrowsException_WhenClassroomNotFound() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(999L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> classroomService.getClassroomHeader("999"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ===================== resetClassCode =====================

    @Test
    @DisplayName("resetClassCode - thành công khi giáo viên reset mã lớp")
    void resetClassCode_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(100L, 1L))
                .thenReturn(Optional.of(activeClassroom));
        when(classroomRepository.existsByClassCode(anyString())).thenReturn(false);

        classroomService.resetClassCode(100L);

        assertThat(activeClassroom.getClassCode()).isNotNull();
        assertThat(activeClassroom.getClassCode()).hasSize(6);
        verify(classroomRepository).save(activeClassroom);
    }

    @Test
    @DisplayName("resetClassCode - ném exception khi lớp không tồn tại hoặc không phải giáo viên")
    void resetClassCode_ThrowsException_WhenClassroomNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(999L, 1L))
                .thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> classroomService.resetClassCode(999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(classroomRepository, never()).save(any());
    }

    // ===================== updateClassMemberStatus =====================

    @Test
    @DisplayName("updateClassMemberStatus - ném exception khi không phải giáo viên của lớp")
    void updateClassMemberStatus_ThrowsException_WhenNotTeacher() {
        // activeMember thuộc classroom có teacherId = 1
        // studentUser có id = 2 => không phải giáo viên
        when(authService.getCurrentUser()).thenReturn(studentUser);
        when(classMemberRepository.findById(10L)).thenReturn(Optional.of(activeMember));
        when(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN)).thenReturn("Forbidden");

        var request = new com.vn.backend.dto.request.classroom.ClassMemberStatusUpdateRequest();
        request.setClassMemberStatus("INACTIVE");

        assertThatThrownBy(() -> classroomService.updateClassMemberStatus("10", request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("updateClassMemberStatus - ném exception khi thành viên không tồn tại")
    void updateClassMemberStatus_ThrowsException_WhenMemberNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classMemberRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        var request = new com.vn.backend.dto.request.classroom.ClassMemberStatusUpdateRequest();
        request.setClassMemberStatus("INACTIVE");

        assertThatThrownBy(() -> classroomService.updateClassMemberStatus("999", request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                });
    }

    // ===================== getDetailClassroomSetting =====================

    @Test
    @DisplayName("getDetailClassroomSetting - ném exception khi setting lớp không tồn tại")
    void getDetailClassroomSetting_ThrowsException_WhenSettingNotFound() {
        when(classroomSettingRepository.findByClassroomId(100L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        assertThatThrownBy(() -> classroomService.getDetailClassroomSetting("100"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
