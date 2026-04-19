package com.vn.backend;

import com.vn.backend.dto.request.invitation.InvitationSearchRequest;
import com.vn.backend.dto.request.invitation.JoinClassroomByCodeRequest;
import com.vn.backend.dto.request.invitation.RespondInvitationRequest;
import com.vn.backend.dto.request.invitation.SendBulkInvitationRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.entities.*;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.impl.InvitationServiceImpl;
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

import static com.vn.backend.constants.AppConst.MessageConst;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvitationServiceImpl Unit Tests")
class InvitationServiceImplTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private ClassMemberRepository classMemberRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;
    @Mock private NotificationService notificationService;
    @Mock private MessageUtils messageUtils;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    private User teacherUser;
    private User studentTargetUser;
    private Classroom activeClassroom;
    private Invitation pendingInvitation;

    @BeforeEach
    void setUp() {
        teacherUser = User.builder().id(1L).username("teacher").role(Role.TEACHER).build();
        studentTargetUser = User.builder().id(2L).username("student").role(Role.STUDENT).build();
        activeClassroom = Classroom.builder().classroomId(100L).teacherId(1L).classCode("CODE123").classCodeStatus(ClassCodeStatus.ACTIVE).className("Math 101").build();
        
        pendingInvitation = Invitation.builder()
                .invitationId(10L).userId(2L).classroomId(100L)
                .invitationStatus(ClassroomInvitationStatus.PENDING)
                .classroom(activeClassroom).build();

        when(messageUtils.getMessage(anyString())).thenReturn("Error Message");
        when(authService.getCurrentUser()).thenReturn(teacherUser);
        when(classroomRepository.findById(100L)).thenReturn(Optional.of(activeClassroom));
    }

    // ================== sendBulkInvitation ==================
    @Test
    @DisplayName("TC_QLLH_INV_01: sendBulkInvitation - ném FORBIDDEN khi mâu thuẫn Role (User là Teacher mà mời làm Student)")
    void sendBulkInvitation_ConflictRole() {
        SendBulkInvitationRequest request = new SendBulkInvitationRequest();
        request.setClassroomId(100L);
        request.setUserIds(List.of(5L));
        request.setClassMemberRole(ClassMemberRole.STUDENT);

        User inviteeTeacher = User.builder().id(5L).role(Role.TEACHER).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(inviteeTeacher));

        assertThatThrownBy(() -> invitationService.sendBulkInvitation(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_INV_02: sendBulkInvitation - ném FORBIDDEN khi người mời không có quyền (không phải Assistant/Teacher)")
    void sendBulkInvitation_NoPermission() {
        when(authService.getCurrentUser()).thenReturn(studentTargetUser); // ID=2
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 2L)).thenReturn(false);
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(anyLong(), anyLong(), any(), any())).thenReturn(Optional.empty());

        SendBulkInvitationRequest request = new SendBulkInvitationRequest();
        request.setClassroomId(100L);
        request.setUserIds(List.of(3L));

        assertThatThrownBy(() -> invitationService.sendBulkInvitation(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_INV_03: sendBulkInvitation - mời thành công và tạo Notification")
    void sendBulkInvitation_Success() {
        when(classroomRepository.existsByClassroomIdAndTeacherId(100L, 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentTargetUser));
        when(userRepository.existsById(2L)).thenReturn(true);
        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(anyLong(), anyLong(), any())).thenReturn(false);
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(anyLong(), anyLong(), any())).thenReturn(false);
        
        Invitation savedInv = Invitation.builder().invitationId(88L).build();
        when(invitationRepository.save(any(Invitation.class))).thenReturn(savedInv);

        SendBulkInvitationRequest request = new SendBulkInvitationRequest();
        request.setClassroomId(100L);
        request.setUserIds(List.of(2L));
        request.setClassMemberRole(ClassMemberRole.STUDENT);

        invitationService.sendBulkInvitation(request);

        verify(invitationRepository).save(any(Invitation.class));
        verify(notificationService).createNotificationForUser(any(), eq(2L), eq(NotificationObjectType.INVITE_CLASS), eq(88L), any(Invitation.class));
    }

    // ================== joinClassroomByCode ==================
    @Test
    @DisplayName("TC_QLLH_INV_04: joinClassroomByCode - báo lỗi nếu Code không tồn tại")
    void joinByCode_NotFound() {
        when(classroomRepository.findByClassCode("WRONG")).thenReturn(Optional.empty());
        JoinClassroomByCodeRequest request = new JoinClassroomByCodeRequest();
        request.setClassCode("WRONG");

        assertThatThrownBy(() -> invitationService.joinClassroomByCode(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Error Message");
    }

    @Test
    @DisplayName("TC_QLLH_INV_05: joinClassroomByCode - báo lỗi nếu mã Code đã bị vô hiệu hóa")
    void joinByCode_Disabled() {
        activeClassroom.setClassCodeStatus(ClassCodeStatus.DISABLED);
        when(classroomRepository.findByClassCode("CODE123")).thenReturn(Optional.of(activeClassroom));

        JoinClassroomByCodeRequest request = new JoinClassroomByCodeRequest();
        request.setClassCode("CODE123");

        assertThatThrownBy(() -> invitationService.joinClassroomByCode(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_INV_06: joinClassroomByCode - thành công bằng cách Re-activate Member cũ")
    void joinByCode_Success_Reactivate() {
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        when(classroomRepository.findByClassCode("CODE123")).thenReturn(Optional.of(activeClassroom));
        
        // Giả lập user đã từng ở trong lớp (INACTIVE)
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(100L, 2L, ClassMemberStatus.INACTIVE)).thenReturn(true);
        ClassMember oldMember = ClassMember.builder().userId(2L).classroomId(100L).memberStatus(ClassMemberStatus.INACTIVE).build();
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberStatus(100L, 2L, ClassMemberStatus.INACTIVE)).thenReturn(oldMember);

        JoinClassroomByCodeRequest request = new JoinClassroomByCodeRequest();
        request.setClassCode("CODE123");

        invitationService.joinClassroomByCode(request);

        assertThat(oldMember.getMemberStatus()).isEqualTo(ClassMemberStatus.ACTIVE);
        verify(classMemberRepository).save(oldMember);
        verify(notificationService).createNotificationForUser(any(), eq(1L), eq(NotificationObjectType.JOIN_CLASS), eq(100L), any(Invitation.class));
    }

    @Test
    @DisplayName("TC_QLLH_INV_07: joinClassroomByCode - thành công tạo mới Member và gửi Notify cho Assistant")
    void joinByCode_Success_NewMember() {
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        when(classroomRepository.findByClassCode("CODE123")).thenReturn(Optional.of(activeClassroom));
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(anyLong(), anyLong(), any())).thenReturn(false);
        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(anyLong(), anyLong(), any())).thenReturn(false);
        
        // Giả lập trong lớp có 1 trợ giảng
        ClassMember assistant = ClassMember.builder().userId(3L).memberRole(ClassMemberRole.ASSISTANT).build();
        when(classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(100L, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE))
                .thenReturn(List.of(assistant));

        JoinClassroomByCodeRequest request = new JoinClassroomByCodeRequest();
        request.setClassCode("CODE123");

        invitationService.joinClassroomByCode(request);

        verify(classMemberRepository).save(any(ClassMember.class));
        // Gửi cho Teacher (ID=1) và Assistant (ID=3)
        verify(notificationService).createNotificationForUser(any(), eq(1L), any(), anyLong(), any(Classroom.class));
        verify(notificationService).createNotificationForUser(any(), eq(3L), any(), anyLong(), any(Classroom.class));
    }

    // ================== respondToInvitation ==================
    @Test
    @DisplayName("TC_QLLH_INV_08: respondToInvitation - ném FORBIDDEN nếu trả lời lời mời của người khác")
    void respond_Forbidden() {
        when(authService.getCurrentUser()).thenReturn(User.builder().id(99L).build()); // User lân cận
        when(invitationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(pendingInvitation)); // Của ID=2

        RespondInvitationRequest request = new RespondInvitationRequest();
        request.setInvitationId(10L);
        request.setResponseStatus(ClassroomInvitationStatus.ACCEPTED);

        assertThatThrownBy(() -> invitationService.respondToInvitation(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC_QLLH_INV_09: respondToInvitation - ném BAD_REQUEST nếu lời mời k còn pending")
    void respond_AlreadyResponded() {
        pendingInvitation.setInvitationStatus(ClassroomInvitationStatus.DECLINED);
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        when(invitationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(pendingInvitation));

        RespondInvitationRequest request = new RespondInvitationRequest();
        request.setInvitationId(10L);

        assertThatThrownBy(() -> invitationService.respondToInvitation(request))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("TC_QLLH_INV_10: respondToInvitation - ACCEPTED thành công và add user vào lớp")
    void respond_Success_Accepted() {
        pendingInvitation.setMemberRole(ClassMemberRole.STUDENT);
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        when(invitationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(pendingInvitation));
        when(classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(anyLong(), any(), any())).thenReturn(Collections.emptyList());

        RespondInvitationRequest request = new RespondInvitationRequest();
        request.setInvitationId(10L);
        request.setResponseStatus(ClassroomInvitationStatus.ACCEPTED);

        invitationService.respondToInvitation(request);

        assertThat(pendingInvitation.getInvitationStatus()).isEqualTo(ClassroomInvitationStatus.ACCEPTED);
        verify(classMemberRepository).save(any(ClassMember.class));
        verify(invitationRepository).save(pendingInvitation);
    }

    @Test
    @DisplayName("TC_QLLH_INV_11: respondToInvitation - REJECTED thành công, ko add vào lớp")
    void respond_Success_Rejected() {
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        when(invitationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(pendingInvitation));

        RespondInvitationRequest request = new RespondInvitationRequest();
        request.setInvitationId(10L);
        request.setResponseStatus(ClassroomInvitationStatus.DECLINED);

        invitationService.respondToInvitation(request);

        assertThat(pendingInvitation.getInvitationStatus()).isEqualTo(ClassroomInvitationStatus.DECLINED);
        verify(classMemberRepository, never()).save(any(ClassMember.class));
    }

    // ================== Paging List ==================
    @Test
    @DisplayName("TC_QLLH_INV_12: getUserInvitationsWithPagination - thành công")
    void getUserInvitations_Success() {
        when(authService.getCurrentUser()).thenReturn(studentTargetUser);
        
        InvitationSearchRequest req = new InvitationSearchRequest();
        req.setPagination(new SearchRequest());
        
        Page<Invitation> page = new PageImpl<>(List.of(pendingInvitation));
        when(invitationRepository.findByUserIdWithFilters(anyLong(), any(), any(Pageable.class))).thenReturn(page);

        var result = invitationService.getUserInvitationsWithPagination(req);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPaging().getTotalRows()).isEqualTo(1);
    }
}
