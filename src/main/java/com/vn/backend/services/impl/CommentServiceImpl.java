package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.comment.CommentCreateRequest;
import com.vn.backend.dto.request.comment.CommentListRequest;
import com.vn.backend.dto.request.comment.CommentUpdateRequest;
import com.vn.backend.dto.request.comment.CommentDeleteRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.comment.CommentResponse;
import com.vn.backend.entities.*;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AnnouncementRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.CommentRepository;
import com.vn.backend.repositories.ClassroomRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.CommentService;
import com.vn.backend.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final AnnouncementRepository announcementRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final AuthService authService;
    private final MessageUtils messageUtils;

    @Override
    @Transactional
    public CommentResponse createComment(Long announcementId, CommentCreateRequest request) {
        log.info("Creating comment for announcement: {}", announcementId);
        User currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();

        // Validate announcement exists and user has access
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Check if user is member of the classroom
        validateClassroomAccess(announcement.getClassroomId(), currentUserId);

        // Check if comments are allowed for this announcement
        if (!announcement.getAllowComments()) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.COMMENT_NOT_ALLOW), HttpStatus.FORBIDDEN);
        }

        // Create comment
        Comment comment = Comment.builder()
                .announcementId(announcementId)
                .userId(currentUserId)
                .user(currentUser)
                .content(request.getContent())
                .isDeleted(false)
                .build();

        Comment savedComment = commentRepository.saveAndFlush(comment);
        log.info("Successfully created comment with ID: {}", savedComment.getCommentId());

        return mapToResponse(savedComment, currentUser);
    }

    @Override
    public ResponseListData<CommentResponse> getCommentList(CommentListRequest request) {
        log.info("Getting comment list for announcement: {}", request.getAnnouncementId());
        User currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();

        // Validate announcement exists and user has access
        Announcement announcement = announcementRepository.findById(request.getAnnouncementId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Check if user is member of the classroom
        validateClassroomAccess(announcement.getClassroomId(), currentUserId);

        // Create pageable
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();

        // Get comments
        Page<Comment> commentPage = commentRepository.findByAnnouncementIdAndNotDeleted(
                request.getAnnouncementId(), pageable);

        // Map to response
        List<CommentResponse> content = commentPage.getContent().stream()
                .map(comment -> mapToResponse(comment, currentUser))
                .collect(Collectors.toList());

        // Update paging meta
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(commentPage.getTotalElements());
        pagingMeta.setTotalPages(commentPage.getTotalPages());

        log.info("Found {} comments for announcement {}", commentPage.getTotalElements(), request.getAnnouncementId());
        return new ResponseListData<>(content, pagingMeta);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(CommentUpdateRequest request) {
        log.info("Updating comment with ID: {}", request.getCommentId());
        User currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();

        // Find comment
        Comment comment = commentRepository.findByCommentIdAndNotDeleted(request.getCommentId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Validate ownership or admin permission
        boolean check = validatePermission(comment, currentUserId, AppConst.EDIT);

        if (!check) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.BAD_REQUEST);
        }
        // Update content
        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.saveAndFlush(comment);

        log.info("Successfully updated comment with ID: {}", request.getCommentId());
        return mapToResponse(updatedComment, currentUser);
    }

    @Override
    @Transactional
    public void deleteComment(CommentDeleteRequest request) {
        log.info("Deleting comment with ID: {}", request.getCommentId());
        Long currentUserId = authService.getCurrentUser().getId();

        // Find comment
        Comment comment = commentRepository.findByCommentIdAndNotDeleted(request.getCommentId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Validate ownership or admin permission
        boolean check = validatePermission(comment, currentUserId, AppConst.DELETE);

        // Soft delete
        if (check) {
            commentRepository.softDeleteById(request.getCommentId());
            log.info("Successfully deleted comment with ID: {}", request.getCommentId());
            return;
        }
        throw new AppException(AppConst.MessageConst.FORBIDDEN,
                messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
    }

    private void validateClassroomAccess(Long classroomId, Long userId) {
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)) {
            return;
        }
        // Check if user is member of classroom
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, userId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

        if (classMember.getMemberStatus() != ClassMemberStatus.ACTIVE) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
    }


    private CommentResponse mapToResponse(Comment comment, User currentUser) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setAnnouncementId(comment.getAnnouncementId());
        response.setUserId(comment.getUserId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        response.setUserFullName(comment.getUser().getFullName()); // fix
        response.setUserEmail(comment.getUser().getEmail());
        response.setUserAvatar(comment.getUser().getAvatarUrl());

        // Set permissions
        boolean canEdit = validatePermission(comment, currentUser.getId(), AppConst.EDIT);
        boolean canDelete = validatePermission(comment, currentUser.getId(), AppConst.DELETE);

        response.setCanEdit(canEdit);
        response.setCanDelete(canDelete);

        return response;
    }

    private boolean validatePermission(Comment comment, Long userId, String action) {
        Announcement announcement = announcementRepository.findById(comment.getAnnouncementId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        if (comment.getUserId().equals(userId)) {
            return true;
        }
        // Check if user is teacher of the classroom
        if (classroomRepository.existsByClassroomIdAndTeacherId(announcement.getClassroomId(), userId)) {
            return action.equals(AppConst.DELETE);
        }
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(announcement.getClassroomId(), userId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        if (classMember.getMemberRole() == ClassMemberRole.ASSISTANT && action.equals(AppConst.DELETE)) {
            // Assistant can delete comment of students in the same class
            // Determine role of comment owner in this classroom
            if (comment.getUserId().equals(userId)) return true; // own comment
            ClassMember commentOwnerMember = classMemberRepository.findByClassroomIdAndUserId(announcement.getClassroomId(), comment.getUserId())
                    .orElse(null);
            if (commentOwnerMember != null && commentOwnerMember.getMemberRole() == ClassMemberRole.STUDENT) {
                return true;
            }
            return false;
        }
        return false;
    }
}

