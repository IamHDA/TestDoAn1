package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.assignment.*;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.response.assignment.*;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.*;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.*;
import com.vn.backend.services.*;
import com.vn.backend.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.vn.backend.constants.AppConst.TITLE_ANNOUCEMENT_MAP;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AnnouncementRepository announcementRepository;
    private final AttachmentRepository attachmentRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomRepository classroomRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final MessageUtils messageUtils;
    private final AnnouncementService announcementService;
    private final SubmissionService submissionService;
    private final EmailService emailService;
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;
    @Override
    @Transactional
    public Long createAssignment(Long classroomId, AssignmentCreateRequest request) {
        log.info("Creating assignment for classroom: {}", classroomId);
        Long currentUserId = authService.getCurrentUser().getId();
        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatus(classroomId, ClassroomStatus.ACTIVE).orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        // Validate user access to classroom
        validateEditPermission(classroomId, currentUserId);
        // Create assignment linked to announcement
        Assignment assignment = Assignment.builder()
                .classroomId(classroomId)
                .title(request.getTitle())
                .content(request.getContent())
                .maxScore(request.getMaxScore())
                .dueDate(request.getDueDate())
                .submissionClosed(Boolean.TRUE.equals(request.getSubmissionClosed()))
                .createdBy(currentUserId)
                .build();
        assignmentRepository.saveAndFlush(assignment);

        // Create announcement
        Announcement announcement = Announcement.builder()
                .classroomId(classroomId)
                .title(String.format(TITLE_ANNOUCEMENT_MAP.get(TitleAnnouncementType.ASSIGNMENT), authService.getCurrentUser().getFullName(), classroom.getClassName()))
                .type(AnnouncementType.ASSIGNMENT)
                .allowComments(request.getAllowComments())
                .createdBy(currentUserId)
                .isDeleted(false)
                .objectId(assignment.getAssignmentId())
                .build();

        announcementRepository.save(announcement);
        // Create attachments if any (polymorphic association)
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<Attachment> attachments = request.getAttachments().stream()
                    .map(attachmentRequest -> Attachment.builder()
                            .objectId(assignment.getAssignmentId()) // Link to assignment
                            .attachmentType(AttachmentType.ASSIGNMENT) // Set attachment type
                            .fileName(attachmentRequest.getFileName())
                            .fileUrl(attachmentRequest.getFileUrl())
                            .description(attachmentRequest.getDescription())
                            .uploadedBy(currentUserId)
                            .isDeleted(false)
                            .build())
                    .collect(Collectors.toList());

            attachmentRepository.saveAllAndFlush(attachments);
        }


        announcementService.notifyAnnouncement(announcement);

        submissionService.createDefaultSubmissions(assignment);

        // Gửi email thông báo cho sinh viên trong lớp
//        sendMailAssignment(assignment, classroom);

        log.info("Successfully created assignment with ID: {} and announcement with ID: {}",
                assignment.getAssignmentId(), announcement.getAnnouncementId());
        return assignment.getAssignmentId();
    }

    /**
     * Gửi email thông báo cho tất cả sinh viên trong lớp khi có bài tập mới
     */
    private void sendMailAssignment(Assignment assignment, Classroom classroom) {
        try {
            // Lấy danh sách tất cả sinh viên trong lớp
            List<ClassMember> classMembers = classMemberRepository.findByClassroomIdAndMemberRoleAndMemberStatus(
                classroom.getClassroomId(), ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE);
            
            if (classMembers.isEmpty()) {
                log.info("No students found in classroom {} to send assignment notification", classroom.getClassroomId());
                return;
            }

            // Lấy thông tin user của các sinh viên
            List<Long> studentIds = classMembers.stream()
                .map(ClassMember::getUserId)
                .toList();
            
            List<User> students = userRepository.findAllByIdInAndIsDeletedFalse(studentIds);
            
            if (students.isEmpty()) {
                log.info("No valid students found for classroom {} to send assignment notification", classroom.getClassroomId());
                return;
            }

            // Gửi email cho từng sinh viên trong lớp
            emailService.sendAssignmentCreatedEmail(assignment,students,frontendUrl);
            log.info("Sent assignment notification emails for assignment {} to {} students", 
                assignment.getAssignmentId(), students.size());

        } catch (Exception e) {
            log.error("Error sending assignment notification emails for assignment {}: {}", 
                assignment.getAssignmentId(), e.getMessage());
        }
    }
    @Override
    public AssignmentResponse getAssignmentDetail(Long assignmentId) {
        log.info("Getting assignment detail for assignmentId: {}", assignmentId);
        User currentUser = authService.getCurrentUser();
        Long currentUserId = currentUser.getId();

        Assignment assignment = assignmentRepository.findByAssignmentIdAndNotDeleted(assignmentId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Validate user access to classroom
        validateClassroomAccess(assignment.getClassroomId(), currentUserId);

        ClassMemberRole userRole = getUserRoleInClassroom(assignment.getClassroomId(), currentUserId);

        return mapToResponse(assignment, currentUser, userRole);
    }

    @Override
    @Transactional
    public void updateAssignment(Long assignmentId, AssignmentUpdateRequest request) {
        log.info("Updating assignment with ID: {}", assignmentId);
        Long currentUserId = authService.getCurrentUser().getId();

        Assignment assignment = assignmentRepository.findByAssignmentIdAndNotDeleted(assignmentId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        Announcement announcement = announcementRepository.findByObjectIdAndType(assignmentId,AnnouncementType.ASSIGNMENT);
        // Validate user access to classroom and ownership
        validateClassroomAccess(assignment.getClassroomId(), currentUserId);
        validateEditPermission(assignment.getClassroomId(), currentUserId);

        // Update fields
        if (request.getTitle() != null) {
            assignment.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            assignment.setContent(request.getContent());
        }
        if (request.getMaxScore() != null) {
            assignment.setMaxScore(request.getMaxScore());
        }
        if (request.getDueDate() != null) {
            assignment.setDueDate(request.getDueDate());
        }
        if (request.getSubmissionClosed() != null) {
            assignment.setSubmissionClosed(request.getSubmissionClosed());
        }
        if (request.getAllowComments() != null) {
            announcement.setAllowComments(request.getAllowComments());
        }
        // Cập nhật attachments
        if (request.getAttachments() != null) {
            updateAttachments(assignment, request.getAttachments());
        }
        announcementRepository.save(announcement);
        assignmentRepository.save(assignment);
        log.info("Successfully updated assignment with ID: {}", assignmentId);
    }

    private void updateAttachments(Assignment assigment, List<AssignmentUpdateRequest.AttachmentRequest> newAttachments) {
        // Lấy tất cả attachments hiện tại (bao gồm cả đã xóa)
        List<Attachment> existingAttachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(assigment.getAssignmentId(),AttachmentType.mapToAttachmentType(AnnouncementType.ASSIGNMENT));
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
        for (AssignmentUpdateRequest.AttachmentRequest newAttachment : newAttachments) {
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
                        .objectId(assigment.getAssignmentId())
                        .fileName(newAttachment.getFileName())
                        .fileUrl(newAttachment.getFileUrl())
                        .attachmentType(AttachmentType.mapToAttachmentType(AnnouncementType.ASSIGNMENT))
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

    private void validateClassroomAccess(Long classroomId, Long currentUserId) {

        // Check if user is teacher of classroom
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, currentUserId)) {
            return;
        }

        // Check if user is member of classroom (including Student)
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, currentUserId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

        if (classMember.getMemberStatus() != ClassMemberStatus.ACTIVE) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
    }

    private void validateEditPermission(Long classroomId, Long currentUserId) {

        // Check if user is teacher of classroom
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, currentUserId)) {
            return;
        }

        classMemberRepository.findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(classroomId, currentUserId, ClassMemberRole.ASSISTANT, ClassMemberStatus.ACTIVE)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));

    }

    private ClassMemberRole getUserRoleInClassroom(Long classroomId, Long userId) {
        if (classroomRepository.existsByClassroomIdAndTeacherId(classroomId, userId)) {
            return null; // Teacher doesn't have role in ClassMember
        }
        ClassMember classMember = classMemberRepository.findByClassroomIdAndUserId(classroomId, userId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.FORBIDDEN,
                        messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN));
        return classMember.getMemberRole();
    }


    private AssignmentResponse mapToResponse(Assignment assignment, User currentUser, ClassMemberRole userRole) {
        Announcement announcement = announcementRepository.findByObjectIdAndType(assignment.getAssignmentId(),AnnouncementType.ASSIGNMENT);
        AssignmentResponse response = new AssignmentResponse();
        response.setAssignmentId(assignment.getAssignmentId());
        response.setTitle(assignment.getTitle());
        response.setContent(assignment.getContent());
        response.setClassroomId(assignment.getClassroomId());
        response.setCreatedBy(assignment.getCreatedBy());
        response.setMaxScore(assignment.getMaxScore());
        response.setDueDate(assignment.getDueDate());
        response.setSubmissionClosed(assignment.isSubmissionClosed());
        response.setAllowComments(announcement.getAllowComments());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        response.setAnnouncementId(announcement.getAnnouncementId());
        response.setCreatedByAvatar(assignment.getCreatedByUser().getAvatarUrl());
        response.setCreatedByEmail(assignment.getCreatedByUser().getEmail());
        response.setCreatedByFullName(assignment.getCreatedByUser().getFullName());
        // Set permissions
        boolean canEdit = false;
        boolean canDelete = false;

        if (classroomRepository.existsByClassroomIdAndTeacherId(assignment.getClassroomId(), currentUser.getId())) {
            canEdit = true;
            canDelete = true;
        } else if (userRole == ClassMemberRole.ASSISTANT) {
            canEdit = true;
            canDelete = true;
        }

        // Chỉ Student mới được nộp bài
        boolean canSubmit;

        if ((assignment.getDueDate() != null && assignment.getDueDate().isBefore(LocalDateTime.now())) && !assignment.isSubmissionClosed()) {
            canSubmit = false;
        }
        canSubmit = userRole == ClassMemberRole.STUDENT;
        response.setCanSubmit(canSubmit);        // Check submissionClosed và dueDate

        response.setCanEdit(canEdit);
        response.setCanDelete(canDelete);
        response.setCanSubmit(canSubmit);
        // Set attachments using polymorphic query
        List<Attachment> attachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(
                assignment.getAssignmentId(), AttachmentType.ASSIGNMENT);

        if (attachments != null && !attachments.isEmpty()) {
            List<AssignmentResponse.AttachmentResponse> attachmentResponses = attachments.stream()
                    .map(attachment -> AssignmentResponse.AttachmentResponse.builder()
                            .attachmentId(attachment.getAttachmentId())
                            .fileName(attachment.getFileName())
                            .fileUrl(attachment.getFileUrl())
                            .description(attachment.getDescription())
                            .build())
                    .collect(Collectors.toList());
            response.setAttachments(attachmentResponses);
        }

        return response;
    }

    @Override
    @Transactional
    public void softDeleteAssignment(Long assignmentId) {
        log.info("Soft deleting assignment with ID: {}", assignmentId);
        Long currentUserId = authService.getCurrentUser().getId();

        Assignment assignment = assignmentRepository.findByAssignmentIdAndNotDeleted(assignmentId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Validate user access to classroom and ownership
        validateClassroomAccess(assignment.getClassroomId(), currentUserId);
        validateEditPermission(assignment.getClassroomId(), currentUserId);

        // Soft delete assignment
        assignment.setDeleted(true);
        assignmentRepository.save(assignment);

        // Also soft delete the associated announcement
        Announcement announcement = announcementRepository.findByObjectIdAndType(assignment.getAssignmentId(),AnnouncementType.ASSIGNMENT);
            announcement.setIsDeleted(true);
            announcementRepository.save(announcement);
        log.info("Successfully soft deleted assignment with ID: {}", assignmentId);
    }

    @Override
    public ResponseListData<AssignmentListResponse> getAssignmentList(Long classroomId, AssignmentListRequest request) {
        log.info("Getting assignment list for classroom: {}", classroomId);
        Long currentUserId = authService.getCurrentUser().getId();

        // Validate user access to classroom
        validateClassroomAccess(classroomId, currentUserId);

        // Get pagination info
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();

        // Query with pagination
        Page<Assignment> assignmentPage = assignmentRepository.findByClassroomIdAndNotDeletedWithPagination(classroomId, pageable);

        // Convert to response
        List<AssignmentListResponse> assignments = assignmentPage.getContent().stream()
                .map(assignment -> AssignmentListResponse.builder()
                        .assignmentId(assignment.getAssignmentId())
                        .title(assignment.getTitle())
                        .createdAt(assignment.getCreatedAt())
                        .updatedAt(assignment.getUpdatedAt())
                        .dueDate(assignment.getDueDate())
                        .build())
                .collect(Collectors.toList());

        // Create pagination meta
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(assignmentPage.getTotalElements());
        pagingMeta.setTotalPages(assignmentPage.getTotalPages());

        return new ResponseListData<>(assignments, pagingMeta);
    }

    @Override
    public void addAssignee(String assignmentId, AssigneeAddRequest request) {
        log.info("Start service to add assignee");

        User user = authService.getCurrentUser();
        AssigneeAddRequestDTO dto = request.toDTO();
        Assignment assignment = assignmentRepository.findByAssignmentId(Long.parseLong(assignmentId))
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        this.validateEditPermission(assignment.getClassroomId(), user.getId());
        if (!classMemberRepository.existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(
                assignment.getClassroomId(), dto.getUserId(), ClassMemberStatus.ACTIVE,
                ClassMemberRole.STUDENT)) {
            throw new AppException(AppConst.MessageConst.NOT_FOUND,
                    messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST);
        }
        Optional<Submission> submissionOpt = submissionRepository.findByAssignmentIdAndStudentId(assignment.getAssignmentId(), dto.getUserId());
        if (submissionOpt.isEmpty()) {
            Submission submission = Submission.builder()
                    .assignmentId(assignment.getAssignmentId())
                    .studentId(dto.getUserId())
                    .submissionStatus(SubmissionStatus.NOT_SUBMITTED)
                    .gradingStatus(GradingStatus.NOT_GRADED)
                    .build();
            submissionRepository.save(submission);
        }
        log.info("End service add assignee");
    }

    @Override
    public ResponseListData<AssigneeSearchResponse> searchAssignee(
            BaseFilterSearchRequest<AssigneeSearchRequest> request) {
        log.info("Start service to search assignee");

        User user = authService.getCurrentUser();
        AssigneeSearchRequestDTO dto = request.getFilters().toDTO();
        Assignment assignment = assignmentRepository.findByAssignmentIdAndNotDeleted(dto.getAssignmentId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        this.validateClassroomAccess(assignment.getClassroomId(), user.getId());

        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<AssigneeSearchQueryDTO> queryDTOS = submissionRepository.searchAssignee(dto, pageable);

        List<AssigneeSearchResponse> responseList = queryDTOS.stream()
                .map(AssigneeSearchResponse::fromDTO)
                .toList();
        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());

        log.info("End service search assignee");
        return new ResponseListData<>(responseList, pagingMeta);
    }

    @Override
    public AssignmentStatisticResponse getAssignmentStatistics(String assignmentId) {
        log.info("Start service to get assignment statistics");

        Long assId = Long.parseLong(assignmentId);
        User user = authService.getCurrentUser();

        if (!assignmentRepository.canUserViewSubmissions(assId, user.getId())) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN);
        }
        List<ScoreDistributionQueryDTO> scoreDistributionQueryDTOS = this.getScoreDistribution(assId);
        AssignmentOverviewQueryDTO assignmentOverviewQueryDTO = submissionRepository.getAssignmentOverview(assId);
        AssignmentStatisticResponse response = AssignmentStatisticResponse.builder()
                .overview(AssignmentOverviewResponse.fromDTO(assignmentOverviewQueryDTO))
                .scoreDistribution(
                        scoreDistributionQueryDTOS.stream().map(ScoreDistributionResponse::fromDTO).toList()
                )
                .build();

        log.info("End service get assignment statistics");
        return response;
    }

    private List<ScoreDistributionQueryDTO> getScoreDistribution(Long assignmentId) {
        return submissionRepository.findAllGradesByAssignmentId(assignmentId).stream()
                .collect(Collectors.groupingBy(
                        GradeRange::fromGrade,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(e -> new ScoreDistributionQueryDTO(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentAverageScoreComparisonResponse getAverageScoreComparison(Long classroomId) {
        classroomRepository.findById(classroomId)
                .orElseThrow(() -> new AppException(
                        AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                        HttpStatus.BAD_REQUEST));

        // Query DB trực tiếp để lấy assignments có submissions đã được chấm điểm
        List<Assignment> assignments = assignmentRepository.findAssignmentsWithGradedSubmissionsByClassroomId(classroomId);

        List<AssignmentAverageScoreComparisonResponse.AverageScoreItem> items = new ArrayList<>();
        for (Assignment assignment : assignments) {
            // Query DB trực tiếp để lấy average score
            Double average = submissionRepository.getAverageGradeByAssignmentId(assignment.getAssignmentId());

            if (average != null) {
                items.add(AssignmentAverageScoreComparisonResponse.AverageScoreItem.builder()
                        .label(assignment.getTitle())
                        .value(roundToTwoDecimals(average))
                        .build());
            }
        }

        return AssignmentAverageScoreComparisonResponse.builder()
                .data(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentImprovementTrendResponse getImprovementTrend(
            Long classroomId, StatisticsPeriod period, StatisticsGroupBy groupBy) {
        // Validate classroom
        Classroom classroom = classroomRepository.findByClassroomIdAndClassroomStatusAndIsActiveTrue(classroomId, ClassroomStatus.ACTIVE)
                .orElseThrow(() -> new AppException(
                        AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND),
                        HttpStatus.BAD_REQUEST));

        // Set defaults
        if (period == null) {
            period = StatisticsPeriod.ALL;
        }
        if (groupBy == null) {
            groupBy = StatisticsGroupBy.SESSION;
        }

        // Calculate date range based on period
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStartDate = null;
        if (period == StatisticsPeriod.MONTH) {
            periodStartDate = now.minusMonths(1);
        } else if (period == StatisticsPeriod.QUARTER) {
            periodStartDate = now.minusMonths(3);
        } else if (period == StatisticsPeriod.SEMESTER) {
            periodStartDate = now.minusMonths(6);
        }
        final LocalDateTime finalStartDate = periodStartDate;

        // Query DB trực tiếp để lấy assignments
        List<Assignment> assignments = assignmentRepository.findAllByClassroomIdAndIsDeletedFalse(classroomId);

        // Filter by period and dueDate < now (only completed assignments)
        List<Assignment> filteredAssignments = assignments.stream()
                .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(now))
                .filter(a -> finalStartDate == null || a.getDueDate().isAfter(finalStartDate) || a.getDueDate().isEqual(finalStartDate))
                .sorted(Comparator.comparing(Assignment::getCreatedAt))
                .collect(Collectors.toList());

        // Group data based on groupBy
        List<AssignmentImprovementTrendResponse.TrendDataItem> trendData = groupTrendData(
                filteredAssignments, groupBy);

        // Calculate overall trend
        AssignmentImprovementTrendResponse.OverallTrend overallTrend = calculateOverallTrend(trendData);

        // Calculate statistics
        AssignmentImprovementTrendResponse.TrendStatistics statistics = calculateTrendStatistics(
                filteredAssignments, trendData);

        return AssignmentImprovementTrendResponse.builder()
                .classroomId(classroomId)
                .classroomName(classroom.getClassName())
                .period(period)
                .groupBy(groupBy)
                .trendData(trendData)
                .overallTrend(overallTrend)
                .statistics(statistics)
                .build();
    }

    private List<AssignmentImprovementTrendResponse.TrendDataItem> groupTrendData(
            List<Assignment> assignments, StatisticsGroupBy groupBy) {
        if (groupBy == StatisticsGroupBy.SESSION) {
            return assignments.stream()
                    .map(this::createTrendDataItemFromAssignment)
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
        } else if (groupBy == StatisticsGroupBy.WEEK) {
            return groupByWeek(assignments);
        } else if (groupBy == StatisticsGroupBy.MONTH) {
            return groupByMonth(assignments);
        }
        // Default to session
        return assignments.stream()
                .map(this::createTrendDataItemFromAssignment)
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    private AssignmentImprovementTrendResponse.TrendDataItem createTrendDataItemFromAssignment(
            Assignment assignment) {
        Double averageScore = submissionRepository.getAverageGradeByAssignmentId(assignment.getAssignmentId());
        if (averageScore == null) {
            return null;
        }
        List<Double> scores = submissionRepository.findAllGradesByAssignmentId(assignment.getAssignmentId());
        double medianScore = calculateMedian(scores);


        Long totalStudents = submissionRepository.countTotalStudentsByAssignmentId(assignment.getAssignmentId());
        Long submittedStudents = submissionRepository.countSubmittedStudentsByAssignmentId(assignment.getAssignmentId());

        Double passRate = submissionRepository.getPassRateByAssignmentId(assignment.getAssignmentId());
        Double excellentRate = submissionRepository.getExcellentRateByAssignmentId(assignment.getAssignmentId());

        return AssignmentImprovementTrendResponse.TrendDataItem.builder()
                .period(assignment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .periodLabel(assignment.getTitle())
                .assignmentId(assignment.getAssignmentId())
                .assignmentTitle(assignment.getTitle())
                .averageScore(roundToTwoDecimals(averageScore))
                .medianScore(roundToTwoDecimals(medianScore))
                .totalStudents(totalStudents != null ? totalStudents.intValue() : 0)
                .submittedStudents(submittedStudents != null ? submittedStudents.intValue() : 0)
                .passRate(roundToTwoDecimals(passRate != null ? passRate : 0.0))
                .excellentRate(roundToTwoDecimals(excellentRate != null ? excellentRate : 0.0))
                .build();
    }

    private List<AssignmentImprovementTrendResponse.TrendDataItem> groupByWeek(
            List<Assignment> assignments) {
        Map<String, List<Assignment>> groupedByWeek = new LinkedHashMap<>();

        for (Assignment a : assignments) {
            LocalDateTime createdAt = a.getCreatedAt();
            // Get start of week (Monday)
            LocalDateTime weekStart = createdAt.minusDays(createdAt.getDayOfWeek().getValue() - 1);
            String weekKey = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE);

            groupedByWeek.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(a);
        }

        return groupedByWeek.entrySet().stream()
                .map(entry -> createTrendDataItemFromAssignments(entry.getKey(),
                        "Tuần " + entry.getKey(), entry.getValue()))
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    private List<AssignmentImprovementTrendResponse.TrendDataItem> groupByMonth(
            List<Assignment> assignments) {
        Map<String, List<Assignment>> groupedByMonth = new LinkedHashMap<>();

        for (Assignment a : assignments) {
            LocalDateTime createdAt = a.getCreatedAt();
            String monthKey = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            groupedByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(a);
        }

        return groupedByMonth.entrySet().stream()
                .map(entry -> createTrendDataItemFromAssignments(entry.getKey(),
                        "Tháng " + entry.getKey(), entry.getValue()))
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    private AssignmentImprovementTrendResponse.TrendDataItem createTrendDataItemFromAssignments(
            String period, String periodLabel, List<Assignment> assignments) {
        List<Double> allScores = new ArrayList<>();
        int totalStudents = 0;
        int submittedStudents = 0;

        for (Assignment a : assignments) {
            // Query DB trực tiếp để lấy scores
            List<Double> scores = submissionRepository.findAllGradesByAssignmentId(a.getAssignmentId());
            allScores.addAll(scores);

            // Query DB trực tiếp để lấy total và submitted students
            Long total = submissionRepository.countTotalStudentsByAssignmentId(a.getAssignmentId());
            if (total != null) {
                totalStudents += total.intValue();
            }

            Long submitted = submissionRepository.countSubmittedStudentsByAssignmentId(a.getAssignmentId());
            if (submitted != null) {
                submittedStudents += submitted.intValue();
            }
        }

        if (allScores.isEmpty()) {
            return null;
        }

        // Tính toán từ dữ liệu đã query
        double averageScore = allScores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double medianScore = calculateMedian(allScores);

        long passCount = allScores.stream().filter(s -> s >= 5.0).count();
        long excellentCount = allScores.stream().filter(s -> s >= 8.0).count();
        double passRate = allScores.size() > 0 ? (double) passCount / allScores.size() * 100 : 0.0;
        double excellentRate = allScores.size() > 0 ? (double) excellentCount / allScores.size() * 100 : 0.0;

        return AssignmentImprovementTrendResponse.TrendDataItem.builder()
                .period(period)
                .periodLabel(periodLabel)
                .assignmentId(null) // Multiple assignments
                .assignmentTitle(periodLabel)
                .averageScore(roundToTwoDecimals(averageScore))
                .medianScore(roundToTwoDecimals(medianScore))
                .totalStudents(totalStudents)
                .submittedStudents(submittedStudents)
                .passRate(roundToTwoDecimals(passRate))
                .excellentRate(roundToTwoDecimals(excellentRate))
                .build();
    }

    private AssignmentImprovementTrendResponse.OverallTrend calculateOverallTrend(
            List<AssignmentImprovementTrendResponse.TrendDataItem> trendData) {
        if (trendData.isEmpty()) {
            return AssignmentImprovementTrendResponse.OverallTrend.builder()
                    .firstAverageScore(0.0)
                    .lastAverageScore(0.0)
                    .improvement(0.0)
                    .trend(ImprovementTrend.STABLE)
                    .build();
        }

        double firstScore = trendData.get(0).getAverageScore();
        double lastScore = trendData.get(trendData.size() - 1).getAverageScore();

        double improvement = firstScore > 0
                ? ((lastScore - firstScore) / firstScore) * 100
                : 0.0;

        ImprovementTrend trend;
        if (improvement > 1.0) {
            trend = ImprovementTrend.IMPROVING;
        } else if (improvement < -1.0) {
            trend = ImprovementTrend.DECLINING;
        } else {
            trend = ImprovementTrend.STABLE;
        }

        return AssignmentImprovementTrendResponse.OverallTrend.builder()
                .firstAverageScore(roundToTwoDecimals(firstScore))
                .lastAverageScore(roundToTwoDecimals(lastScore))
                .improvement(roundToTwoDecimals(improvement))
                .trend(trend)
                .build();
    }

    private AssignmentImprovementTrendResponse.TrendStatistics calculateTrendStatistics(
            List<Assignment> assignments,
            List<AssignmentImprovementTrendResponse.TrendDataItem> trendData) {
        int totalAssignments = trendData.size();

        // Calculate average improvement
        double averageImprovement = 0.0;
        if (trendData.size() > 1) {
            double totalImprovement = 0.0;
            for (int i = 1; i < trendData.size(); i++) {
                double prev = trendData.get(i - 1).getAverageScore();
                double curr = trendData.get(i).getAverageScore();
                if (prev > 0) {
                    totalImprovement += ((curr - prev) / prev) * 100;
                }
            }
            averageImprovement = totalImprovement / (trendData.size() - 1);
        }

        // Calculate consistent improvers and declining students
        // Query DB trực tiếp để lấy scores của từng student
        int consistentImprovers = 0;
        int decliningStudents = 0;

        Map<Long, List<Double>> studentScores = new HashMap<>();
        for (Assignment a : assignments) {
            List<Submission> submissions = submissionRepository.findAllByAssignmentId(a.getAssignmentId());

            for (Submission s : submissions) {
                if (s.getGrade() != null && (s.getSubmissionStatus() == SubmissionStatus.SUBMITTED
                        || s.getSubmissionStatus() == SubmissionStatus.LATE_SUBMITTED)) {
                    studentScores.computeIfAbsent(s.getStudentId(), k -> new ArrayList<>())
                            .add(s.getGrade());
                }
            }
        }

        for (List<Double> scores : studentScores.values()) {
            if (scores.size() >= 2) {
                boolean isImproving = true;
                boolean isDeclining = true;

                for (int i = 1; i < scores.size(); i++) {
                    if (scores.get(i) < scores.get(i - 1)) {
                        isImproving = false;
                    }
                    if (scores.get(i) > scores.get(i - 1)) {
                        isDeclining = false;
                    }
                }

                if (isImproving && !isDeclining) {
                    consistentImprovers++;
                } else if (isDeclining && !isImproving) {
                    decliningStudents++;
                }
            }
        }

        return AssignmentImprovementTrendResponse.TrendStatistics.builder()
                .totalAssignments(totalAssignments)
                .averageImprovement(roundToTwoDecimals(averageImprovement))
                .consistentImprovers(consistentImprovers)
                .decliningStudents(decliningStudents)
                .build();
    }

    private double calculateMedian(List<Double> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator.naturalOrder());

        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
