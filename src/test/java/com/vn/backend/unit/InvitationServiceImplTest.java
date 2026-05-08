package com.vn.backend.unit;

import com.vn.backend.dto.request.invitation.JoinClassroomByCodeRequest;
import com.vn.backend.dto.request.invitation.SendBulkInvitationRequest;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.Classroom;
import com.vn.backend.entities.Invitation;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.repositories.InvitationRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.impl.InvitationServiceImpl;
import com.vn.backend.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationServiceImplTest {

    private static final Long CLASSROOM_ID = 10L;
    private static final Long TEACHER_ID = 4L;
    private static final Long ASSISTANT_ID = 5L;
    private static final Long STUDENT_ID = 8L;
    private static final String CLASS_CODE = "ABC123";

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private ClassMemberRepository classMemberRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationService notificationService;

    private InvitationServiceImpl service;

    private final List<Invitation> savedInvitations = new ArrayList<>();
    private final List<ClassMember> savedClassMembers = new ArrayList<>();
    private final AtomicLong invitationIds = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        MessageUtils messageUtils = ServiceTestSupport.mockMessageUtils();

        service = new InvitationServiceImpl(
                messageUtils,
                invitationRepository,
                classMemberRepository,
                classroomRepository,
                userRepository,
                authService,
                notificationService
        );

        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
            Invitation invitation = invocation.getArgument(0);

            if (invitation.getInvitationId() == null) {
                invitation.setInvitationId(invitationIds.getAndIncrement());
            }

            savedInvitations.add(invitation);
            return invitation;
        });

        when(classMemberRepository.save(any(ClassMember.class))).thenAnswer(invocation -> {
            ClassMember classMember = invocation.getArgument(0);
            savedClassMembers.add(classMember);
            return classMember;
        });
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .role(role)
                .fullName("User " + id)
                .email("user" + id + "@example.com")
                .avatarUrl("avatar-" + id + ".png")
                .build();
    }

    private void mockCurrentUser(Long userId, Role role) {
        when(authService.getCurrentUser()).thenReturn(user(userId, role));
    }

    private Classroom classroom() {
        return Classroom.builder()
                .classroomId(CLASSROOM_ID)
                .className("SE Class")
                .classCode(CLASS_CODE)
                .classCodeStatus(ClassCodeStatus.ACTIVE)
                .teacherId(TEACHER_ID)
                .build();
    }

    private Classroom classroomWithCodeStatus(ClassCodeStatus status) {
        return Classroom.builder()
                .classroomId(CLASSROOM_ID)
                .className("SE Class")
                .classCode(CLASS_CODE)
                .classCodeStatus(status)
                .teacherId(TEACHER_ID)
                .build();
    }

    private ClassMember member(Long userId, ClassMemberRole role, ClassMemberStatus status) {
        return ClassMember.builder()
                .classroomId(CLASSROOM_ID)
                .userId(userId)
                .memberRole(role)
                .memberStatus(status)
                .joinedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private SendBulkInvitationRequest sendBulkRequest(List<Long> userIds, ClassMemberRole role) {
        SendBulkInvitationRequest request = new SendBulkInvitationRequest();
        request.setClassroomId(CLASSROOM_ID);
        request.setUserIds(userIds);
        request.setClassMemberRole(role);
        return request;
    }

    private JoinClassroomByCodeRequest joinByCodeRequest(String code) {
        JoinClassroomByCodeRequest request = new JoinClassroomByCodeRequest();
        request.setClassCode(code);
        return request;
    }

    private void mockClassroomFindByIdExists() {
        when(classroomRepository.findById(CLASSROOM_ID)).thenReturn(Optional.of(classroom()));
        when(classroomRepository.existsById(CLASSROOM_ID)).thenReturn(true);
    }

    private void mockClassroomFindByIdMissing() {
        when(classroomRepository.findById(CLASSROOM_ID)).thenReturn(Optional.empty());
    }

    private void mockTeacherPermission(Long userId, boolean isTeacher) {
        when(classroomRepository.existsByClassroomIdAndTeacherId(CLASSROOM_ID, userId))
                .thenReturn(isTeacher);
    }

    private void mockAssistantPermission(Long userId, boolean isAssistant) {
        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberRole.ASSISTANT,
                ClassMemberStatus.ACTIVE
        )).thenReturn(isAssistant ? Optional.of(member(userId, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE)) : Optional.empty());
    }

    private void mockUserExists(Long userId, Role role) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, role)));
        when(userRepository.existsById(userId)).thenReturn(true);
    }

    private void mockUserMissing(Long userId) {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.existsById(userId)).thenReturn(false);
    }

    private void mockNoExistingActiveMember(Long userId) {
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberStatus.ACTIVE
        )).thenReturn(false);
    }

    private void mockExistingActiveMember(Long userId) {
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberStatus.ACTIVE
        )).thenReturn(true);
    }

    private void mockNoPendingOrAcceptedInvitation(Long userId) {
        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                CLASSROOM_ID,
                userId,
                ClassroomInvitationStatus.PENDING
        )).thenReturn(false);

        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                CLASSROOM_ID,
                userId,
                ClassroomInvitationStatus.ACCEPTED
        )).thenReturn(false);
    }

    private void mockPendingInvitationExists(Long userId) {
        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                CLASSROOM_ID,
                userId,
                ClassroomInvitationStatus.PENDING
        )).thenReturn(true);
    }

    private void mockAcceptedInvitationExists(Long userId) {
        when(invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                CLASSROOM_ID,
                userId,
                ClassroomInvitationStatus.ACCEPTED
        )).thenReturn(true);
    }

    private void mockJoinClassroomByCode(ClassCodeStatus status) {
        when(classroomRepository.findByClassCode(CLASS_CODE))
                .thenReturn(Optional.of(classroomWithCodeStatus(status)));
    }

    private void mockJoinClassroomCodeMissing() {
        when(classroomRepository.findByClassCode(CLASS_CODE))
                .thenReturn(Optional.empty());
    }

    private void mockNoInactiveMember(Long userId) {
        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberStatus.INACTIVE
        )).thenReturn(false);
    }

    private ClassMember mockInactiveMember(Long userId) {
        ClassMember inactiveMember = member(userId, ClassMemberRole.STUDENT, ClassMemberStatus.INACTIVE);

        when(classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberStatus.INACTIVE
        )).thenReturn(true);

        when(classMemberRepository.findByClassroomIdAndUserIdAndMemberStatus(
                CLASSROOM_ID,
                userId,
                ClassMemberStatus.INACTIVE
        )).thenReturn(inactiveMember);

        return inactiveMember;
    }

    private void mockAssistantsInClassroom(List<ClassMember> assistants) {
        when(classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(
                CLASSROOM_ID,
                ClassMemberRole.ASSISTANT,
                ClassMemberStatus.ACTIVE
        )).thenReturn(assistants);
    }

    @Nested
    class SendBulkInvitationTests {

        @Test
        void sendBulkInvitation_Success_TeacherInvitesStudents() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();
            mockTeacherPermission(TEACHER_ID, true);
            mockAssistantPermission(TEACHER_ID, false);

            mockUserExists(STUDENT_ID, Role.STUDENT);
            mockNoExistingActiveMember(STUDENT_ID);
            mockNoPendingOrAcceptedInvitation(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(1, savedInvitations.size());

            Invitation invitation = savedInvitations.get(0);
            assertEquals(CLASSROOM_ID, invitation.getClassroomId());
            assertEquals(STUDENT_ID, invitation.getUserId());
            assertEquals(ClassroomInvitationType.INVITE, invitation.getInvitationType());
            assertEquals(ClassMemberRole.STUDENT, invitation.getMemberRole());
            assertEquals(TEACHER_ID, invitation.getInvitedBy());
            assertEquals(ClassroomInvitationStatus.PENDING, invitation.getInvitationStatus());

            verify(notificationService).createNotificationForUser(
                    any(User.class),
                    eq(STUDENT_ID),
                    eq(NotificationObjectType.INVITE_CLASS),
                    eq(invitation.getInvitationId()),
                    eq(invitation)
            );
        }

        @Test
        void sendBulkInvitation_Success_AssistantInvitesStudent() {
            mockCurrentUser(ASSISTANT_ID, Role.STUDENT);
            mockClassroomFindByIdExists();
            mockTeacherPermission(ASSISTANT_ID, false);
            mockAssistantPermission(ASSISTANT_ID, true);

            mockUserExists(STUDENT_ID, Role.STUDENT);
            mockNoExistingActiveMember(STUDENT_ID);
            mockNoPendingOrAcceptedInvitation(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(1, savedInvitations.size());
            assertEquals(ASSISTANT_ID, savedInvitations.get(0).getInvitedBy());
        }

        @Test
        void sendBulkInvitation_Fail_ThrowsWhenClassroomMissing() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdMissing();

            assertThrows(AppException.class, () ->
                    service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT))
            );

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void sendBulkInvitation_Fail_ThrowsWhenSenderIsNotTeacherOrAssistant() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockClassroomFindByIdExists();
            mockTeacherPermission(STUDENT_ID, false);
            mockAssistantPermission(STUDENT_ID, false);

            mockUserExists(20L, Role.STUDENT);

            assertThrows(AppException.class, () ->
                    service.sendBulkInvitation(sendBulkRequest(List.of(20L), ClassMemberRole.STUDENT))
            );

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void sendBulkInvitation_Fail_ThrowsWhenAssistantInvitesAssistant() {
            mockCurrentUser(ASSISTANT_ID, Role.STUDENT);
            mockClassroomFindByIdExists();
            mockTeacherPermission(ASSISTANT_ID, false);
            mockAssistantPermission(ASSISTANT_ID, true);

            mockUserExists(20L, Role.TEACHER);

            assertThrows(AppException.class, () ->
                    service.sendBulkInvitation(sendBulkRequest(List.of(20L), ClassMemberRole.ASSISTANT))
            );

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void sendBulkInvitation_Fail_ThrowsWhenTeacherUserInvitedAsStudent() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();

            mockUserExists(20L, Role.TEACHER);

            assertThrows(AppException.class, () ->
                    service.sendBulkInvitation(sendBulkRequest(List.of(20L), ClassMemberRole.STUDENT))
            );

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void sendBulkInvitation_Fail_ThrowsWhenStudentUserInvitedAsAssistant() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();

            mockUserExists(20L, Role.STUDENT);

            assertThrows(AppException.class, () ->
                    service.sendBulkInvitation(sendBulkRequest(List.of(20L), ClassMemberRole.ASSISTANT))
            );

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        void sendBulkInvitation_SkipsMissingUser() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();
            mockTeacherPermission(TEACHER_ID, true);
            mockAssistantPermission(TEACHER_ID, false);

            mockUserMissing(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(0, savedInvitations.size());
            verify(notificationService, never()).createNotificationForUser(
                    any(User.class),
                    anyLong(),
                    any(NotificationObjectType.class),
                    anyLong(),
                    any(Invitation.class)
            );
        }

        @Test
        void sendBulkInvitation_SkipsAlreadyActiveMember() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();
            mockTeacherPermission(TEACHER_ID, true);
            mockAssistantPermission(TEACHER_ID, false);

            mockUserExists(STUDENT_ID, Role.STUDENT);
            mockExistingActiveMember(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(0, savedInvitations.size());
        }

        @Test
        void sendBulkInvitation_SkipsWhenPendingInvitationExists() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();
            mockTeacherPermission(TEACHER_ID, true);
            mockAssistantPermission(TEACHER_ID, false);

            mockUserExists(STUDENT_ID, Role.STUDENT);
            mockNoExistingActiveMember(STUDENT_ID);
            mockPendingInvitationExists(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(0, savedInvitations.size());
        }

        @Test
        void sendBulkInvitation_SkipsWhenAcceptedInvitationExists() {
            mockCurrentUser(TEACHER_ID, Role.TEACHER);
            mockClassroomFindByIdExists();
            mockTeacherPermission(TEACHER_ID, true);
            mockAssistantPermission(TEACHER_ID, false);

            mockUserExists(STUDENT_ID, Role.STUDENT);
            mockNoExistingActiveMember(STUDENT_ID);
            mockAcceptedInvitationExists(STUDENT_ID);

            service.sendBulkInvitation(sendBulkRequest(List.of(STUDENT_ID), ClassMemberRole.STUDENT));

            assertEquals(0, savedInvitations.size());
        }
    }

    @Nested
    class JoinClassroomByCodeTests {

        @Test
        void joinClassroomByCode_Success_CreatesNewClassMemberAndNotifiesTeacherAndAssistants() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomByCode(ClassCodeStatus.ACTIVE);
            mockNoExistingActiveMember(STUDENT_ID);
            mockNoPendingOrAcceptedInvitation(STUDENT_ID);
            mockNoInactiveMember(STUDENT_ID);

            ClassMember assistant = member(ASSISTANT_ID, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE);
            mockAssistantsInClassroom(List.of(assistant));

            service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE));

            assertEquals(1, savedClassMembers.size());

            ClassMember savedMember = savedClassMembers.get(0);
            assertEquals(CLASSROOM_ID, savedMember.getClassroomId());
            assertEquals(STUDENT_ID, savedMember.getUserId());
            assertEquals(ClassMemberRole.STUDENT, savedMember.getMemberRole());
            assertEquals(ClassMemberStatus.ACTIVE, savedMember.getMemberStatus());
            assertNotNull(savedMember.getJoinedAt());

            verify(notificationService).createNotificationForUser(
                    any(User.class),
                    eq(TEACHER_ID),
                    eq(NotificationObjectType.JOIN_CLASS),
                    eq(CLASSROOM_ID),
                    any(Classroom.class)
            );

            verify(notificationService).createNotificationForUser(
                    any(User.class),
                    eq(ASSISTANT_ID),
                    eq(NotificationObjectType.JOIN_CLASS),
                    eq(CLASSROOM_ID),
                    any(Classroom.class)
            );
        }

        @Test
        void joinClassroomByCode_Success_ReactivatesInactiveMember() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomByCode(ClassCodeStatus.ACTIVE);
            mockNoExistingActiveMember(STUDENT_ID);
            mockNoPendingOrAcceptedInvitation(STUDENT_ID);

            ClassMember inactiveMember = mockInactiveMember(STUDENT_ID);
            mockAssistantsInClassroom(List.of());

            service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE));

            assertEquals(ClassMemberStatus.ACTIVE, inactiveMember.getMemberStatus());
            assertEquals(ClassMemberRole.STUDENT, inactiveMember.getMemberRole());
            assertNotNull(inactiveMember.getJoinedAt());

            verify(classMemberRepository).save(eq(inactiveMember));
        }

        @Test
        void joinClassroomByCode_Fail_ThrowsWhenClassCodeNotFound() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomCodeMissing();

            assertThrows(AppException.class, () ->
                    service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE))
            );

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }

        @Test
        void joinClassroomByCode_Fail_ThrowsWhenClassCodeDisabled() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomByCode(ClassCodeStatus.DISABLED);

            assertThrows(AppException.class, () ->
                    service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE))
            );

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }

        @Test
        void joinClassroomByCode_Fail_ThrowsWhenUserAlreadyActiveMember() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomByCode(ClassCodeStatus.ACTIVE);
            mockExistingActiveMember(STUDENT_ID);

            assertThrows(AppException.class, () ->
                    service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE))
            );

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }

        @Test
        void joinClassroomByCode_Fail_ThrowsWhenPendingInvitationExists() {
            mockCurrentUser(STUDENT_ID, Role.STUDENT);
            mockJoinClassroomByCode(ClassCodeStatus.ACTIVE);
            mockNoExistingActiveMember(STUDENT_ID);
            mockPendingInvitationExists(STUDENT_ID);

            assertThrows(AppException.class, () ->
                    service.joinClassroomByCode(joinByCodeRequest(CLASS_CODE))
            );

            verify(classMemberRepository, never()).save(any(ClassMember.class));
        }
    }
}