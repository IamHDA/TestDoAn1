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
    @DisplayName("TC_QLLH_01: getDetailClassroom - thành công khi lớp tồn tại")
    void getDetailClassroom_Success() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));

        var result = classroomService.getDetailClassroom("100");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("TC_QLLH_02: getDetailClassroom - ném exception khi lớp không tồn tại")
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
    @DisplayName("TC_QLLH_03: getClassroomHeader - thành công khi lớp tồn tại")
    void getClassroomHeader_Success() {
        when(classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(100L, ClassroomStatus.ACTIVE))
                .thenReturn(Optional.of(activeClassroom));

        var result = classroomService.getClassroomHeader("100");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("TC_QLLH_04: getClassroomHeader - ném exception khi lớp không tồn tại")
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
    @DisplayName("TC_QLLH_05: resetClassCode - thành công khi giáo viên reset mã lớp")
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
    @DisplayName("TC_QLLH_06: resetClassCode - ném exception khi lớp không tồn tại hoặc không phải giáo viên")
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
    @DisplayName("TC_QLLH_07: updateClassMemberStatus - ném exception khi không phải giáo viên của lớp")
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
    @DisplayName("TC_QLLH_08: updateClassMemberStatus - ném exception khi thành viên không tồn tại")
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
    @DisplayName("TC_QLLH_09: getDetailClassroomSetting - ném exception khi setting lớp không tồn tại")
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

    // ===================== createClassroom =====================

    @Test
    @DisplayName("TC_QLLH_10: createClassroom - thành công, gọi đủ các repository liên quan")
    void createClassroom_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        com.vn.backend.dto.request.classroom.ClassroomCreateRequest request = new com.vn.backend.dto.request.classroom.ClassroomCreateRequest();
        request.setSubjectId("1");
        request.setRequestDescription("Test Request");
        
        com.vn.backend.dto.request.classroom.ClassScheduleRequest scheduleReq = new com.vn.backend.dto.request.classroom.ClassScheduleRequest();
        scheduleReq.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        scheduleReq.setStartTime(java.time.LocalTime.parse("08:00"));
        scheduleReq.setEndTime(java.time.LocalTime.parse("10:00"));
        scheduleReq.setRoom("B101");
        request.setClassSchedules(java.util.List.of(scheduleReq));

        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(1L)).thenReturn(true);
        when(classroomRepository.existsByClassCode(anyString())).thenReturn(false);

        classroomService.createClassroom(request);

        verify(classroomRepository).save(any(Classroom.class));
        verify(classroomSettingRepository).save(any(com.vn.backend.entities.ClassroomSetting.class));
        verify(classScheduleRepository).saveAll(anyList());
        verify(approvalRequestService).createRequest(eq(RequestType.CLASS_CREATE), eq("Test Request"), eq(1L), anyList());
    }

    @Test
    @DisplayName("TC_QLLH_11: createClassroom - ném exception khi subject không tồn tại")
    void createClassroom_ThrowsException_WhenSubjectNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        com.vn.backend.dto.request.classroom.ClassroomCreateRequest request = new com.vn.backend.dto.request.classroom.ClassroomCreateRequest();
        request.setSubjectId("99");
        
        when(subjectRepository.existsBySubjectIdAndIsDeletedIsFalse(99L)).thenReturn(false);
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");
        // class code generation mocked
        when(classroomRepository.existsByClassCode(anyString())).thenReturn(false);

        assertThatThrownBy(() -> classroomService.createClassroom(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("TC_QLLH_12: createClassroom - ném exception khi gen code thất bại sau MAX_TRIES")
    void createClassroom_ThrowsException_MaxTriesClassCode() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        com.vn.backend.dto.request.classroom.ClassroomCreateRequest request = new com.vn.backend.dto.request.classroom.ClassroomCreateRequest();
        
        // Luôn trả về true => mã code luôn bị trùng => vòng lặp sẽ chạy MAX_TRIES lần rồi throw exception
        when(classroomRepository.existsByClassCode(anyString())).thenReturn(true);
        when(messageUtils.getMessage(AppConst.MessageConst.ERR_CLASS_CODE_GENERATION_FAILED)).thenReturn("Code gen failed");

        assertThatThrownBy(() -> classroomService.createClassroom(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.ERR_CLASS_CODE_GENERATION_FAILED);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    // ===================== searchClassroom =====================

    @Test
    @DisplayName("TC_QLLH_13: searchClassroom - trả về trang dữ liệu thành công")
    void searchClassroom_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        com.vn.backend.dto.request.common.BaseFilterSearchRequest<com.vn.backend.dto.request.classroom.ClassroomSearchRequest> filterReq = new com.vn.backend.dto.request.common.BaseFilterSearchRequest<>();
        filterReq.setFilters(new com.vn.backend.dto.request.classroom.ClassroomSearchRequest());
        
        com.vn.backend.dto.request.common.SearchRequest pagination = new com.vn.backend.dto.request.common.SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        filterReq.setPagination(pagination);

        com.vn.backend.dto.response.classroom.ClassroomSearchQueryDTO mockDTO = org.mockito.Mockito.mock(com.vn.backend.dto.response.classroom.ClassroomSearchQueryDTO.class);
        org.springframework.data.domain.Page<com.vn.backend.dto.response.classroom.ClassroomSearchQueryDTO> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(mockDTO));
        
        when(classroomRepository.searchClassroom(any(), any())).thenReturn(mockPage);

        var result = classroomService.searchClassroom(filterReq);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }

    // ===================== updateClassroom =====================

    @Test
    @DisplayName("TC_QLLH_14: updateClassroom - thành công kèm cập nhật lịch học")
    void updateClassroom_Success_WithSchedules() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(100L, 1L)).thenReturn(Optional.of(activeClassroom));

        com.vn.backend.dto.request.classroom.ClassroomUpdateRequest request = new com.vn.backend.dto.request.classroom.ClassroomUpdateRequest();
        request.setClassName("Updated Name");
        
        com.vn.backend.dto.request.classroom.ClassScheduleRequest scheduleReq = new com.vn.backend.dto.request.classroom.ClassScheduleRequest();
        scheduleReq.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        request.setClassSchedules(java.util.List.of(scheduleReq));

        classroomService.updateClassroom("100", request);

        assertThat(activeClassroom.getClassName()).isEqualTo("Updated Name");
        assertThat(activeClassroom.getSchedules()).hasSize(1);
        verify(classroomRepository).save(activeClassroom);
    }

    @Test
    @DisplayName("TC_QLLH_15: updateClassroom - thành công không cập nhật lịch học")
    void updateClassroom_Success_NoSchedules() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(100L, 1L)).thenReturn(Optional.of(activeClassroom));

        com.vn.backend.dto.request.classroom.ClassroomUpdateRequest request = new com.vn.backend.dto.request.classroom.ClassroomUpdateRequest();
        request.setClassName("Updated Name 2");
        request.setClassSchedules(null);

        classroomService.updateClassroom("100", request);

        assertThat(activeClassroom.getClassName()).isEqualTo("Updated Name 2");
        verify(classroomRepository).save(activeClassroom);
    }

    @Test
    @DisplayName("TC_QLLH_16: updateClassroom - ném exception khi không tìm thấy lớp")
    void updateClassroom_ThrowsException_WhenNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findByClassroomIdAndTeacherIdAndIsActiveTrue(999L, 1L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        com.vn.backend.dto.request.classroom.ClassroomUpdateRequest request = new com.vn.backend.dto.request.classroom.ClassroomUpdateRequest();

        assertThatThrownBy(() -> classroomService.updateClassroom("999", request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.NOT_FOUND);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ===================== updateClassroomSetting =====================

    @Test
    @DisplayName("TC_QLLH_17: updateClassroomSetting - thành công")
    void updateClassroomSetting_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        com.vn.backend.entities.ClassroomSetting setting = com.vn.backend.entities.ClassroomSetting.builder().classroomId(100L).allowStudentPost(false).build();
        when(classroomSettingRepository.findByClassroomIdAndTeacherId(100L, 1L)).thenReturn(Optional.of(setting));

        com.vn.backend.dto.request.classroom.ClassroomSettingUpdateRequest request = new com.vn.backend.dto.request.classroom.ClassroomSettingUpdateRequest();
        request.setAllowStudentPost(true);

        classroomService.updateClassroomSetting("100", request);

        assertThat(setting.getAllowStudentPost()).isTrue();
        verify(classroomSettingRepository).save(setting);
    }

    @Test
    @DisplayName("TC_QLLH_18: updateClassroomSetting - ném exception khi không tìm thấy setting")
    void updateClassroomSetting_ThrowsException_WhenNotFound() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomSettingRepository.findByClassroomIdAndTeacherId(999L, 1L)).thenReturn(Optional.empty());
        when(messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND)).thenReturn("Not found");

        com.vn.backend.dto.request.classroom.ClassroomSettingUpdateRequest request = new com.vn.backend.dto.request.classroom.ClassroomSettingUpdateRequest();
        
        assertThatThrownBy(() -> classroomService.updateClassroomSetting("999", request))
                .isInstanceOf(AppException.class);
    }
    
    // ===================== getDetailClassroomSetting =====================

    @Test
    @DisplayName("TC_QLLH_19: getDetailClassroomSetting - thành công")
    void getDetailClassroomSetting_Success() {
        com.vn.backend.entities.ClassroomSetting setting = com.vn.backend.entities.ClassroomSetting.builder().classroomId(100L).allowStudentPost(false).build();
        when(classroomSettingRepository.findByClassroomId(100L)).thenReturn(Optional.of(setting));

        var result = classroomService.getDetailClassroomSetting("100");

        assertThat(result).isNotNull();
    }

    // ===================== searchClassMember =====================

    @Test
    @DisplayName("TC_QLLH_20: searchClassMember - thành công khi user là giáo viên hoặc member")
    void searchClassMember_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        
        // Mock method isMemberOrTeacherOfClass
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);

        com.vn.backend.dto.request.common.BaseFilterSearchRequest<com.vn.backend.dto.request.classroom.ClassMemberSearchRequest> filterReq = new com.vn.backend.dto.request.common.BaseFilterSearchRequest<>();
        com.vn.backend.dto.request.classroom.ClassMemberSearchRequest searchReq = new com.vn.backend.dto.request.classroom.ClassMemberSearchRequest();
        searchReq.setClassroomId("100");
        filterReq.setFilters(searchReq);

        com.vn.backend.dto.request.common.SearchRequest pagination = new com.vn.backend.dto.request.common.SearchRequest();
        pagination.setPageNum("1");
        pagination.setPageSize("10");
        filterReq.setPagination(pagination);

        com.vn.backend.dto.response.classroom.ClassMemberSearchQueryDTO mockDTO = org.mockito.Mockito.mock(com.vn.backend.dto.response.classroom.ClassMemberSearchQueryDTO.class);
        org.springframework.data.domain.Page<com.vn.backend.dto.response.classroom.ClassMemberSearchQueryDTO> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(mockDTO));
        
        when(classMemberRepository.searchClassMember(any(), any())).thenReturn(mockPage);

        var result = classroomService.searchClassMember(filterReq);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("TC_QLLH_21: searchClassMember - ném exception FORBIDDEN khi user không liên quan lớp học")
    void searchClassMember_ThrowsException_WhenForbidden() {
        when(authService.getCurrentUser()).thenReturn(studentUser); // SV 2
        
        // Không phải teacher
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        // CŨng không phải member
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(100L, 2L, ClassMemberStatus.ACTIVE)).thenReturn(false);
        when(messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN)).thenReturn("Forbidden");

        com.vn.backend.dto.request.common.BaseFilterSearchRequest<com.vn.backend.dto.request.classroom.ClassMemberSearchRequest> filterReq = new com.vn.backend.dto.request.common.BaseFilterSearchRequest<>();
        com.vn.backend.dto.request.classroom.ClassMemberSearchRequest searchReq = new com.vn.backend.dto.request.classroom.ClassMemberSearchRequest();
        searchReq.setClassroomId("100");
        filterReq.setFilters(searchReq);

        assertThatThrownBy(() -> classroomService.searchClassMember(filterReq))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getCode()).isEqualTo(AppConst.MessageConst.FORBIDDEN);
                    assertThat(appEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    // ===================== updateClassMemberStatus =====================
    
    @Test
    @DisplayName("TC_QLLH_22: updateClassMemberStatus - thành công")
    void updateClassMemberStatus_Success() {
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classMemberRepository.findById(10L)).thenReturn(Optional.of(activeMember));

        var request = new com.vn.backend.dto.request.classroom.ClassMemberStatusUpdateRequest();
        request.setClassMemberStatus(com.vn.backend.enums.ClassMemberStatus.INACTIVE.name());

        classroomService.updateClassMemberStatus("10", request);

        assertThat(activeMember.getMemberStatus()).isEqualTo(ClassMemberStatus.INACTIVE);
        verify(classMemberRepository).save(activeMember);
    }
}
