package com.vn.backend.services.impl;

import com.vn.backend.dto.request.invitation.InvitationFilterRequest;
import com.vn.backend.dto.request.invitation.InvitationSearchRequest;
import com.vn.backend.dto.request.invitation.JoinClassroomByCodeRequest;
import com.vn.backend.dto.request.invitation.RespondInvitationRequest;
import com.vn.backend.dto.request.invitation.SendBulkInvitationRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.invitation.InvitationResponse;
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
import com.vn.backend.services.InvitationService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.enums.NotificationObjectType;
import com.vn.backend.utils.MessageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.vn.backend.constants.AppConst.MessageConst;

@Service
public class InvitationServiceImpl extends BaseService implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public InvitationServiceImpl(MessageUtils messageUtils, InvitationRepository invitationRepository,
                                 ClassMemberRepository classMemberRepository, ClassroomRepository classroomRepository,
                                 UserRepository userRepository, AuthService authService, NotificationService notificationService) {
        super(messageUtils);
        this.invitationRepository = invitationRepository;
        this.classMemberRepository = classMemberRepository;
        this.classroomRepository = classroomRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }


    @Override
    @Transactional
    public void sendBulkInvitation(SendBulkInvitationRequest request) {
        User user = authService.getCurrentUser();
        Long teacherId = user.getId();
        log.info("Sending bulk invitation from user {} to {} users for classroom {}", 
                teacherId, request.getUserIds().size(), request.getClassroomId());
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new AppException(messageUtils.getMessage(MessageConst.CLASSROOM_NOT_FOUND),
                messageUtils.getMessage(MessageConst.CLASSROOM_NOT_FOUND), HttpStatus.BAD_REQUEST));
        // Kiểm tra lại list userIds có ai có Role là Teacher nhưng ClassMemberRole là STUDENT không hoặc có Role là STUDENT nhưng ClassMemberRole là ASSISTANT nếu có trả ra lỗi 
        for (Long userId : request.getUserIds()) {
            User userCheck = userRepository.findById(userId).orElse(null);
            if (userCheck == null) {
                continue;
            }
            // Kiểm tra mâu thuẫn giữa Role và ClassMemberRole
            if ((userCheck.getRole() == Role.TEACHER && request.getClassMemberRole() == ClassMemberRole.STUDENT) || 
                (userCheck.getRole() == Role.STUDENT && request.getClassMemberRole() == ClassMemberRole.ASSISTANT)) {
                throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED),
                        messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED), HttpStatus.FORBIDDEN);
            }
        }
        // Kiểm tra classroom có tồn tại không
        if (!classroomRepository.existsById(request.getClassroomId())) {
            throw new AppException(messageUtils.getMessage(MessageConst.CLASSROOM_NOT_FOUND),
                    messageUtils.getMessage(MessageConst.CLASSROOM_NOT_FOUND), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra quyền mời: Teacher hoặc Assistant trong lớp
        boolean isTeacher = classroomRepository.existsByClassroomIdAndTeacherId(
                request.getClassroomId(), teacherId);
        boolean isAssistant = classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                request.getClassroomId(), teacherId, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE).isPresent();
        
        if (!isTeacher && !isAssistant) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED),
                    messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED), HttpStatus.FORBIDDEN);
        }
        
        // Nếu là Assistant, chỉ được mời STUDENT
        if (isAssistant && !isTeacher && request.getClassMemberRole() != ClassMemberRole.STUDENT) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED),
                    messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED), HttpStatus.FORBIDDEN);
        }

        for (Long userId : request.getUserIds()) {
                // Kiểm tra user có tồn tại không
                if (!userRepository.existsById(userId)) {
                    continue;
                }

                // Kiểm tra user đã là member của classroom chưa
                if (classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(request.getClassroomId(), userId,ClassMemberStatus.ACTIVE)) {
                    continue;
                }

                // Kiểm tra đã có lời mời hoặc đã ở trong classroom chưa
                if (invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                        request.getClassroomId(), userId, ClassroomInvitationStatus.PENDING) ||
                        invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                                request.getClassroomId(), userId, ClassroomInvitationStatus.ACCEPTED) ||
                                classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(request.getClassroomId(), userId, ClassMemberStatus.ACTIVE)) {
                    continue;
                }

                // Tạo lời mời
                Invitation invitation = Invitation.builder()
                        .classroomId(request.getClassroomId())
                        .userId(userId)
                        .invitationType(ClassroomInvitationType.INVITE)
                        .memberRole(request.getClassMemberRole())
                        .invitedBy(teacherId)
                        .invitationStatus(ClassroomInvitationStatus.PENDING)
                        .classroom(classroom)
                        .build();

                Invitation savedInvitation = invitationRepository.save(invitation);
                // Tạo notification cho user được mời
                notificationService.createNotificationForUser(
                    user,
                    userId,
                    NotificationObjectType.INVITE_CLASS,
                    savedInvitation.getInvitationId(),
                    savedInvitation
                );
        }
    }

    @Override
    @Transactional
    public void joinClassroomByCode(JoinClassroomByCodeRequest request) {
        Long userId = authService.getCurrentUser().getId();

        // Tìm classroom bằng class code
        Classroom classroom = classroomRepository.findByClassCode(request.getClassCode())
                .orElse(null);

        if (classroom == null) {
            throw new AppException(MessageConst.CLASS_NOT_FOUND,
                    messageUtils.getMessage(MessageConst.CLASS_NOT_FOUND), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra ClassCodeStatus
        if (classroom.getClassCodeStatus() != ClassCodeStatus.ACTIVE) {
            throw new AppException(MessageConst.CLASS_CODE_DISABLED,
                    messageUtils.getMessage(MessageConst.CLASS_CODE_DISABLED), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra user đã là member của classroom chưa
        if (classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(classroom.getClassroomId(), userId,ClassMemberStatus.ACTIVE)) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_USER_ALREADY_MEMBER),
                    messageUtils.getMessage(MessageConst.INVITATION_USER_ALREADY_MEMBER), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra đã có lời mời pending chưa
        if (invitationRepository.existsByClassroomIdAndUserIdAndInvitationStatus(
                classroom.getClassroomId(), userId, ClassroomInvitationStatus.PENDING)) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_PENDING_EXISTS),
                    messageUtils.getMessage(MessageConst.INVITATION_PENDING_EXISTS), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra xem user đã là member trước đây trong classroom chưa (trạng thái inactive)
        if (classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(
                classroom.getClassroomId(), userId, ClassMemberStatus.INACTIVE)) {
            ClassMember existingMember = classMemberRepository.findByClassroomIdAndUserIdAndMemberStatus(
                    classroom.getClassroomId(), userId, ClassMemberStatus.INACTIVE);
            existingMember.setMemberStatus(ClassMemberStatus.ACTIVE);
            existingMember.setMemberRole(ClassMemberRole.STUDENT);
            existingMember.setJoinedAt(LocalDateTime.now());
            classMemberRepository.save(existingMember);
        } else {
            // Tạo ClassMember mới
            ClassMember classMember = ClassMember.builder()
                    .classroomId(classroom.getClassroomId())
                    .userId(userId)
                    .memberRole(ClassMemberRole.STUDENT)
                    .joinedAt(LocalDateTime.now())
                    .memberStatus(ClassMemberStatus.ACTIVE)
                    .build();

            classMemberRepository.save(classMember);
        }
        
        // Tạo notification cho teacher và trợ giảng trong lớp
        User currentUser = authService.getCurrentUser();
        
        // Gửi notification cho teacher
        notificationService.createNotificationForUser(
            currentUser,
            classroom.getTeacherId(),
            NotificationObjectType.JOIN_CLASS,
            classroom.getClassroomId(),
            classroom
        );
        
        // Gửi notification cho tất cả trợ giảng (ASSISTANT) trong lớp
        List<ClassMember> assistants = classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(
            classroom.getClassroomId(), 
            ClassMemberRole.ASSISTANT, 
            ClassMemberStatus.ACTIVE
        );
        
        for (ClassMember assistant : assistants) {
            notificationService.createNotificationForUser(
                currentUser,
                assistant.getUserId(),
                NotificationObjectType.JOIN_CLASS,
                classroom.getClassroomId(),
                classroom
            );
        }



    }

    @Override
    @Transactional
    public void respondToInvitation(RespondInvitationRequest request) {
        User user = authService.getCurrentUser();
        Long userId = user.getId();
        log.info("User {} responding to invitation {} with status {}", 
                userId, request.getInvitationId(), request.getResponseStatus());

        // Tìm lời mời
        Invitation invitation = invitationRepository.findByIdWithDetails(request.getInvitationId())
                .orElse(null);
        
        if (invitation == null) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_NOT_FOUND),
                    messageUtils.getMessage(MessageConst.INVITATION_NOT_FOUND), HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra user có phải là người được mời không
        if (!invitation.getUserId().equals(userId)) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED),
                    messageUtils.getMessage(MessageConst.INVITATION_PERMISSION_DENIED), HttpStatus.FORBIDDEN);
        }

        // Kiểm tra lời mời có đang pending không
        if (invitation.getInvitationStatus() != ClassroomInvitationStatus.PENDING) {
            throw new AppException(messageUtils.getMessage(MessageConst.INVITATION_ALREADY_RESPONDED),
                    messageUtils.getMessage(MessageConst.INVITATION_ALREADY_RESPONDED), HttpStatus.BAD_REQUEST);
        }

        // Cập nhật trạng thái lời mời
        invitation.setInvitationStatus(request.getResponseStatus());
        invitation.setRespondedAt(LocalDateTime.now());
        // Nếu chấp nhận, thêm user vào classroom
        if (request.getResponseStatus() == ClassroomInvitationStatus.ACCEPTED) {
            addUserToClassroom(invitation);
            
            // Tạo notification cho teacher và trợ giảng khi user chấp nhận lời mời
            Classroom classroom = invitation.getClassroom();
            
            // Gửi notification cho teacher
            notificationService.createNotificationForUser(
                user,
                classroom.getTeacherId(),
                NotificationObjectType.JOIN_CLASS,
                classroom.getClassroomId(),
                invitation
            );
            List<ClassMember> assistants = null;
            if(user.getRole().equals(Role.TEACHER)) {
                // Gửi notification cho tất cả trợ giảng (ASSISTANT) trong lớp trừ chính họ
                assistants = classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatusAndUserIdNot(
                        classroom.getClassroomId(),
                        ClassMemberRole.ASSISTANT,
                        ClassMemberStatus.ACTIVE,
                        userId
                );
            }
            else {
                // Gửi notification cho tất cả trợ giảng (ASSISTANT) trong lớp
                assistants = classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(
                        classroom.getClassroomId(),
                        ClassMemberRole.ASSISTANT,
                        ClassMemberStatus.ACTIVE
                );
            }
            
            for (ClassMember assistant : assistants) {
                notificationService.createNotificationForUser(
                    user,
                    assistant.getUserId(),
                    NotificationObjectType.JOIN_CLASS,
                    classroom.getClassroomId(),
                    invitation
                );
            }
        }

        invitation = invitationRepository.save(invitation);

        log.info("Invitation {} responded successfully with status {}", 
                invitation.getInvitationId(), invitation.getInvitationStatus());
    }




    private void addUserToClassroom(Invitation invitation) {
        // Chuyển đổi InvitationRole sang ClassMemberRole
        ClassMemberRole memberRole = invitation.getMemberRole();
        // Kiểm tra xem user đã là member trước đây trong classroom chưa (trạng thái inactive)
        if (classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatus(invitation.getClassroomId(), invitation.getUserId(), ClassMemberStatus.INACTIVE)) {
            ClassMember existingMember = classMemberRepository.findByClassroomIdAndUserIdAndMemberStatus(
                    invitation.getClassroomId(), invitation.getUserId(), ClassMemberStatus.INACTIVE);
            existingMember.setMemberStatus(ClassMemberStatus.ACTIVE);
            existingMember.setMemberRole(memberRole);
            existingMember.setJoinedAt(LocalDateTime.now());
            classMemberRepository.save(existingMember);
        } else {
        ClassMember classMember = ClassMember.builder()
                .classroomId(invitation.getClassroomId())
                .userId(invitation.getUserId())
                .memberRole(memberRole)
                .joinedAt(LocalDateTime.now())
                .memberStatus(ClassMemberStatus.ACTIVE)
                .build();
        classMemberRepository.save(classMember);
        }
        log.info("User {} added to classroom {} with role {}",
                invitation.getUserId(), invitation.getClassroomId(), memberRole);
    }


    @Override
    public ResponseListData<InvitationResponse> getUserInvitationsWithPagination(InvitationSearchRequest request) {
        User user = authService.getCurrentUser();
        Long userId = user.getId();
        log.info("Getting paginated invitations for user {} with filters", userId);

        // Extract filters
        InvitationFilterRequest filters = request.getFilters() != null ? request.getFilters() : new InvitationFilterRequest();
        
        // Extract pagination info
        String pageNum = request.getPagination().getPageNum();
        String pageSize = request.getPagination().getPageSize();

        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        // Query with filters
        Page<Invitation> invitationPage = invitationRepository.findByUserIdWithFilters(
                userId, 
                filters.getStatus(), 
                pageable
        );
        
        // Convert to response
        List<InvitationResponse> content = invitationPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // Create pagination meta
        PagingMeta pagingMeta = new PagingMeta(
                Integer.parseInt(pageNum),
                Integer.parseInt(pageSize)
        );
        pagingMeta.setTotalRows(invitationPage.getTotalElements());
        pagingMeta.setTotalPages(invitationPage.getTotalPages());
        
        return new ResponseListData<>(content, pagingMeta);
    }

    private InvitationResponse convertToResponse(Invitation invitation) {
        return InvitationResponse.builder()
                .invitationId(invitation.getInvitationId())
                .classroomId(invitation.getClassroomId())
                .className(invitation.getClassroom() != null ? invitation.getClassroom().getClassName() : null)
                .classCode(invitation.getClassroom() != null ? invitation.getClassroom().getClassCode() : null)
                .userId(invitation.getUserId())
                .inviterAvatar(invitation.getInviter() != null ? invitation.getInviter().getAvatarUrl() : null)
                .userFullName(invitation.getInvitedUser() != null ? invitation.getInvitedUser().getFullName() : null)
                .userEmail(invitation.getInvitedUser() != null ? invitation.getInvitedUser().getEmail() : null)
                .invitationType(invitation.getInvitationType())
                .memberRole(invitation.getMemberRole())
                .invitedBy(invitation.getInvitedBy())
                .inviterFullName(invitation.getInviter() != null ? invitation.getInviter().getFullName() : null)
                .invitationStatus(invitation.getInvitationStatus())
                .respondedAt(invitation.getRespondedAt())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .build();
    }
}
