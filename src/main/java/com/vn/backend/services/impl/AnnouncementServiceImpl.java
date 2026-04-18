package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.announcement.AnnouncementCreateRequest;
import com.vn.backend.dto.request.announcement.AnnouncementFilterRequest;
import com.vn.backend.dto.request.announcement.AnnouncementListRequest;
import com.vn.backend.dto.request.announcement.AnnouncementUpdateRequest;
import com.vn.backend.dto.response.announcement.AnnouncementResponse;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.*;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.AnnouncementService;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.utils.MessageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.vn.backend.dto.response.announcement.AnnouncementResponse.AttachmentResponse;
import java.util.stream.Collectors;

import static com.vn.backend.constants.AppConst.TITLE_ANNOUCEMENT_MAP;

@Service
public class AnnouncementServiceImpl extends BaseService implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AttachmentRepository attachmentRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ClassroomSettingRepository classroomSettingRepository;

    public AnnouncementServiceImpl(MessageUtils messageUtils, AnnouncementRepository announcementRepository,
                                   AttachmentRepository attachmentRepository, ClassMemberRepository classMemberRepository,
                                   ClassroomRepository classroomRepository,
                                   AuthService authService, NotificationService notificationService,
                                   ClassroomSettingRepository classroomSettingRepository) {
        super(messageUtils);
        this.announcementRepository = announcementRepository;
        this.attachmentRepository = attachmentRepository;
        this.classMemberRepository = classMemberRepository;
        this.classroomRepository = classroomRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.classroomSettingRepository = classroomSettingRepository;
    }
    public void notifyAnnouncement(Announcement announcement) {
        // Tạo notification cho tất cả thành viên trong lớp theo loại announcement
        User currentUser = authService.getCurrentUser();
        NotificationObjectType notificationType = getNotificationTypeByAnnouncementType(announcement.getType());
        notificationService.createNotificationForClass(
                currentUser,
                announcement.getClassroomId(),
                notificationType,
                announcement.getObjectId() != null ? announcement.getObjectId():  announcement.getAnnouncementId()
        );
    }


    @Override
    @Transactional
    public void createAnnouncement(Long classroomId, AnnouncementCreateRequest request) {
        log.info("Creating announcement for classroom: {}", classroomId);
        User currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();
        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatus(classroomId,ClassroomStatus.ACTIVE).orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        // Kiểm tra quyền truy cập classroom
        validateClassroomAccess(classroomId,currentUserId);
        if(classroomSettingRepository.existsByClassroomIdAndAllowStudentPostFalse(classroomId) && !classroomRepository.existsByClassroomIdAndTeacherId(classroomId, currentUserId)) {
            throw new AppException(AppConst.MessageConst.CANNOT_POST,
                    messageUtils.getMessage(AppConst.MessageConst.CANNOT_POST), HttpStatus.BAD_REQUEST);
        }
        if(request.getType() == AnnouncementType.MATERIAL && currentUser.getRole() != Role.TEACHER  ){
           throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
        // Tạo announcement
        Announcement announcement = Announcement.builder()
                .classroomId(classroomId)
                .title(getTitleWithAnnouncementType(request.getType(),classroom,request.getTitle() != null ? request.getTitle() : ""))
                .content(request.getContent())
                .type(request.getType())
                .allowComments(request.getAllowComments())
                .createdBy(currentUserId)
                .isDeleted(false)
                .build();
        
        final Announcement savedAnnouncement = announcementRepository.saveAndFlush(announcement);

        // Tạo attachments nếu có
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Attachment> attachments = request.getAttachments().stream()
                    .map(attachmentRequest -> Attachment.builder()
                            .objectId(savedAnnouncement.getAnnouncementId())
                            .fileName(attachmentRequest.getFileName())
                            .fileUrl(attachmentRequest.getFileUrl())
                            .attachmentType(AttachmentType.mapToAttachmentType(announcement.getType()))
                            .description(attachmentRequest.getDescription())
                            .uploadedBy(currentUserId)
                            .isDeleted(false)
                            .build())
                    .collect(Collectors.toList());
            
            attachmentRepository.saveAll(attachments);
        }
        notifyAnnouncement(savedAnnouncement);
        log.info("Successfully created announcement with ID: {}", savedAnnouncement.getAnnouncementId());
    }

    public String getTitleWithAnnouncementType(AnnouncementType announcementType,Classroom classroom,String title ) {
       return  switch (announcementType) {
            case GENERIC -> title;
           default ->
                   String.format(TITLE_ANNOUCEMENT_MAP.get(TitleAnnouncementType.getType(announcementType)),authService.getCurrentUser().getFullName(),classroom.getClassName());
        };
    }


    @Override
    public ResponseListData<AnnouncementResponse> getAnnouncementList(Long classroomId, AnnouncementListRequest request) {
        log.info("Getting announcement list for classroom: {}", classroomId);
        Long currentUserId =  authService.getCurrentUser().getId();

        // Kiểm tra quyền truy cập classroom
        validateClassroomAccess(classroomId,currentUserId);
        AnnouncementFilterRequest filters = request.getFilters() != null ? request.getFilters() : new AnnouncementFilterRequest();

        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<Announcement> announcementPage = announcementRepository.findByClassroomIdWithFilters(
                    classroomId,
                    filters.getType(),
                    pageable);

        
        ClassMemberRole userRole = getUserRoleInClassroom(classroomId, currentUserId);
        
        List<AnnouncementResponse> announcements = announcementPage.getContent().stream()
                .map(announcement -> mapToResponse(announcement, currentUserId, userRole))
                .collect(Collectors.toList());
        
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(announcementPage.getTotalElements());
        pagingMeta.setTotalPages(announcementPage.getTotalPages());
        
        return new ResponseListData<>(announcements, pagingMeta);
    }

    @Override
    public AnnouncementResponse getAnnouncementDetail(Long announcementId) {
        log.info("Getting announcement detail: {}", announcementId);
        Long currentUserId =  authService.getCurrentUser().getId();
        Announcement announcement = announcementRepository.findByIdAndNotDeleted(announcementId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, 
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        
        // Kiểm tra quyền truy cập classroom
        validateClassroomAccess(announcement.getClassroomId(),currentUserId);
        
        ClassMemberRole userRole = getUserRoleInClassroom(announcement.getClassroomId(), currentUserId);
        
        AnnouncementResponse response = mapToResponse(announcement, currentUserId, userRole);
        
        log.info("Successfully retrieved announcement detail: {}", announcementId);
        return response;
    }

    @Override
    @Transactional
    public void updateAnnouncement(Long announcementId, AnnouncementUpdateRequest request) {
        log.info("Updating announcement: {}", announcementId);
        
        Announcement announcement = announcementRepository.findByIdAndNotDeleted(announcementId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, 
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        
        // Kiểm tra quyền chỉnh sửa
        validateEditPermission(announcement);
        
        // Cập nhật thông tin
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        if (request.getAllowComments() != null) {
            announcement.setAllowComments(request.getAllowComments());
        }
        
        announcementRepository.save(announcement);
        
        // Cập nhật attachments
        if (request.getAttachments() != null) {
            updateAttachments(announcement, request.getAttachments());
        }
        
        log.info("Successfully updated announcement: {}", announcementId);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        log.info("Deleting announcement: {}", announcementId);
        
        Announcement announcement = announcementRepository.findByIdAndNotDeleted(announcementId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND, 
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        
        // Kiểm tra quyền xóa
        validateEditPermission(announcement);
        
        // Soft delete
        announcement.setIsDeleted(true);
        announcementRepository.save(announcement);
        
        log.info("Successfully deleted announcement: {}", announcementId);
    }
    private void validateCreateAnnouncement(Long classroomId, Long currentUserId, AnnouncementCreateRequest request) {
        // Kiểm tra xem user có phải teacher của classroom không
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, currentUserId)) {
            return;
        }

        // Kiểm tra xem user có phải member của classroom không (bao gồm cả Student)
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, currentUserId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

        if (classMember.getMemberStatus() != ClassMemberStatus.ACTIVE) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
    }

    private void validateClassroomAccess(Long classroomId, Long currentUserId) {
        
        // Kiểm tra xem user có phải teacher của classroom không
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, currentUserId)) {
            return;
        }
        // Kiểm tra xem user có phải member của classroom không (bao gồm cả Student)
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, currentUserId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN, 
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));
        
        if (classMember.getMemberStatus() != ClassMemberStatus.ACTIVE) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN, 
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
    }

    private ClassMemberRole getUserRoleInClassroom(Long classroomId, Long userId) {
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)) {
            return null; // Teacher không có role trong ClassMember
        }
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, userId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN, 
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));
        return classMember.getMemberRole();
    }

    private void validateEditPermission(Announcement announcement) {
        Long currentUserId =  authService.getCurrentUser().getId();

        // Kiểm tra xem user có phải teacher của classroom không
        if (classroomRepository.existsByClassroomIdAndTeacherId(announcement.getClassroomId(), currentUserId)) {
            return;
        }
        
        ClassMemberRole userRole = getUserRoleInClassroom(announcement.getClassroomId(), currentUserId);
        
        // Assistant và Student chỉ có thể chỉnh sửa thông báo của mình
        if (userRole == ClassMemberRole.ASSISTANT || userRole == ClassMemberRole.STUDENT) {
            if (!announcement.getCreatedBy().equals(currentUserId)) {
                throw new AppException(AppConst.MessageConst.FORBIDDEN, 
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
            }
            return;
        }
        
        // Các role khác không có quyền chỉnh sửa
        throw new AppException(AppConst.MessageConst.FORBIDDEN, 
                messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
    }

    private AnnouncementResponse mapToResponse(Announcement announcement, Long currentUserId, ClassMemberRole userRole) {
        boolean canEdit = false;
        boolean canDelete = false;
        // check kiểm tra role của user
        if (classroomRepository.existsByClassroomIdAndTeacherId(announcement.getClassroomId(), currentUserId) ||
                ((userRole == ClassMemberRole.ASSISTANT || userRole == ClassMemberRole.STUDENT)
                && announcement.getCreatedBy().equals(currentUserId))) {
            canEdit = true;
            canDelete = true;
        }
        List<Attachment> attachmentEntities = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(announcement.getAnnouncementId(),AttachmentType.mapToAttachmentType(announcement.getType()));

        List<AttachmentResponse> attachments = attachmentEntities != null
                ? attachmentEntities.stream()
                .filter(attachment -> !attachment.getIsDeleted())
                .map(attachment -> AttachmentResponse.builder()
                        .attachmentId(attachment.getAttachmentId())
                        .fileName(attachment.getFileName())
                        .fileUrl(attachment.getFileUrl())
                        .description(attachment.getDescription())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();
        
        String fullName = announcement.getCreatedByUser().getFullName();
        String createdByFullName = (fullName != null && !fullName.isEmpty())
                ? fullName
                : announcement.getCreatedByUser().getUsername();

        return AnnouncementResponse.builder()
                .announcementId(announcement.getAnnouncementId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .avatarUrl(announcement.getCreatedByUser().getAvatarUrl())
                .type(announcement.getType())
                .allowComments(announcement.getAllowComments())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .createdBy(announcement.getCreatedBy())
                .createdByFullName(createdByFullName)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .objectId(announcement.getObjectId())
                .attachments(attachments)
                .build();

    }


    private NotificationObjectType getNotificationTypeByAnnouncementType(AnnouncementType announcementType) {
        return switch (announcementType) {
            case GENERIC -> NotificationObjectType.ANNOUNCEMENT;
            case ASSIGNMENT -> NotificationObjectType.ASSIGNMENT;
            case EXAM -> NotificationObjectType.EXAM_CREATED;
            case MATERIAL -> NotificationObjectType.MATERIAL;
        };
    }

    private void updateAttachments(Announcement announcement, List<AnnouncementUpdateRequest.AttachmentUpdateRequest> newAttachments) {
        // Lấy tất cả attachments hiện tại (bao gồm cả đã xóa)
        List<Attachment> existingAttachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(announcement.getAnnouncementId(),AttachmentType.mapToAttachmentType(announcement.getType()));
        // Nếu không có attachments mới, xóa tất cả attachments cũ
        if (newAttachments == null || newAttachments.isEmpty()) {
            for (Attachment attachment : existingAttachments) {
                if (!attachment.getIsDeleted()) {
                    attachmentRepository.softDeleteById(attachment.getAttachmentId());
                }
            }
            return;
        }
        
        // Tạo map để xem attachments hiện tại theo ID
        Map<Long, Attachment> existingAttachmentMap = existingAttachments.stream()
                .filter(att -> !att.getIsDeleted())
                .collect(Collectors.toMap(Attachment::getAttachmentId, att -> att));
        
        // Xử lý từng attachment trong request
        for (AnnouncementUpdateRequest.AttachmentUpdateRequest newAttachment : newAttachments) {
            if (newAttachment.getAttachmentId() != null) {
                // Attachment đã tồn tại, giữ nguyên không cập nhật gì
                Attachment existingAttachment = existingAttachmentMap.get(newAttachment.getAttachmentId());
                if (existingAttachment != null) {
                    // Đánh dấu đã xử lý (giữ nguyên)
                    existingAttachmentMap.remove(newAttachment.getAttachmentId());
                }
            } else {
                // Attachment mới (attachmentId = null), tạo mới
                Attachment newAttachmentEntity = Attachment.builder()
                        .objectId(announcement.getAnnouncementId())
                        .fileName(newAttachment.getFileName())
                        .fileUrl(newAttachment.getFileUrl())
                        .attachmentType(AttachmentType.mapToAttachmentType(announcement.getType()))
                        .description(newAttachment.getDescription())
                        .uploadedBy( authService.getCurrentUser().getId())
                        .isDeleted(false)
                        .build();
                
                attachmentRepository.save(newAttachmentEntity);
            }
        }
        
        // delete những attachments không còn trong danh sách mới
        for (Attachment attachment : existingAttachmentMap.values()) {
            attachmentRepository.softDeleteById(attachment.getAttachmentId());
        }
    }
}