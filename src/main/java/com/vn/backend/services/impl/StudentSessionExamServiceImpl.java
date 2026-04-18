package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamAddRequest;
import com.vn.backend.dto.request.studentsessionexam.StudentSessionExamSearchRequest;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.studentsessionexam.AvailableStudentResponse;
import com.vn.backend.dto.response.studentsessionexam.StudentSessionExamResponse;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.ClassroomSetting;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomSettingRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.EmailService;
import com.vn.backend.services.NotificationService;
import com.vn.backend.services.StudentSessionExamService;
import com.vn.backend.utils.MessageUtils;
import com.vn.backend.utils.RedisUtils;
import com.vn.backend.utils.SearchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentSessionExamServiceImpl extends BaseService implements StudentSessionExamService {

    private final StudentSessionExamRepository studentSessionExamRepository;
    private final SessionExamRepository sessionExamRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassroomSettingRepository classroomSettingRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public StudentSessionExamServiceImpl(
            MessageUtils messageUtils,
            StudentSessionExamRepository studentSessionExamRepository,
            SessionExamRepository sessionExamRepository,
            ClassMemberRepository classMemberRepository,
            ClassroomSettingRepository classroomSettingRepository,
            AuthService authService,
            NotificationService notificationService,
            EmailService emailService) {
        super(messageUtils);
        this.studentSessionExamRepository = studentSessionExamRepository;
        this.sessionExamRepository = sessionExamRepository;
        this.classMemberRepository = classMemberRepository;
        this.classroomSettingRepository = classroomSettingRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Override
    public ResponseListData<AvailableStudentResponse> searchClassStudentsForSessionExam(
            BaseFilterSearchRequest<StudentSessionExamSearchRequest> request) {
        log.info("Searching ALL students in class with isJoined status (native query).");
        User currentUser = authService.getCurrentUser();
        StudentSessionExamSearchRequest filter = request.getFilters();

        if (filter == null || filter.getSessionExamId() == null) {
            throw new AppException(AppConst.MessageConst.REQUIRED_FIELD_EMPTY,
                    messageUtils.getMessage(AppConst.MessageConst.REQUIRED_FIELD_EMPTY), HttpStatus.BAD_REQUEST);
        }

        SessionExam sessionExam = sessionExamRepository.findById(filter.getSessionExamId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
        if (!sessionExam.getCreatedBy().equals(currentUser.getId())) {
            throw new AppException(AppConst.MessageConst.FORBIDDEN,
                    messageUtils.getMessage(AppConst.MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
        Long classroomId = sessionExam.getClassId();
        
        String keyword = filter.getSearch();
        Pageable pageable = request.getPagination() != null && request.getPagination().getPagingMeta() != null 
            ? request.getPagination().getPagingMeta().toPageable() : Pageable.unpaged();

        // Buộc không truyền sort field vào native query để tránh lỗi ORDER BY không tồn tại
        int pageIdx = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : 20;
        Pageable nativePageable = PageRequest.of(pageIdx, pageSize, Sort.unsorted());

        Page<Object[]> page = classMemberRepository.searchStudentsWithJoinStatus(
            classroomId, filter.getSessionExamId(), keyword != null && !keyword.trim().isEmpty() ? "%"+keyword.trim()+"%" : null, nativePageable
        );

        List<AvailableStudentResponse> items = page.getContent().stream().map(obj -> {
            Long id = ((Number)obj[0]).longValue();
            String fullName = (String)obj[1];
            String username = (String)obj[2];
            String code = (String)obj[3];
            String email = (String)obj[4];
            String avatarUrl = (String)obj[5];
            Boolean joined = ((Number)obj[6]).intValue() == 1;
            return new AvailableStudentResponse(id, fullName, username, code, email, avatarUrl, joined);
        }).collect(Collectors.toList());

        PagingMeta pagingMeta = new PagingMeta(page.getNumber()+1, page.getSize());
        pagingMeta.setTotalRows(page.getTotalElements());
        pagingMeta.setTotalPages(page.getTotalPages());

        return new ResponseListData<>(items, pagingMeta);
    }

    @Override
    @Transactional
    public List<StudentSessionExamResponse> addStudents(StudentSessionExamAddRequest request) {
        log.info("Adding students to session exam");
        User currentUser = authService.getCurrentUser();

        // Validate session exam exists and belongs to teacher
        SessionExam sessionExam = sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(request.getSessionExamId(),currentUser.getId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        if (!SessionExamStatus.NOT_STARTED.equals(sessionExam.getStatus())) {
            throw new AppException(AppConst.MessageConst.VALUE_OUT_OF_RANGE,
                    "It is not possible to add students to the exam session", HttpStatus.BAD_REQUEST);
        }
        List<StudentSessionExamResponse> responses = new ArrayList<>();

        for (Long studentId : request.getStudentIds()) {
            // Validate student is in the classroom
            ClassMember classMember = classMemberRepository
                    .findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(
                            sessionExam.getClassId(),
                            studentId,
                            ClassMemberRole.STUDENT,
                            ClassMemberStatus.ACTIVE
                    )
                    .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                            messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

            if (studentSessionExamRepository.existsBySessionExamIdAndStudentIdAndIsDeletedFalse(
                    request.getSessionExamId(), studentId)) {
                log.warn("Student {} already exists in session exam {}, skipping", studentId, request.getSessionExamId());
                continue;
            }

            StudentSessionExam entity = StudentSessionExam.builder()
                    .sessionExamId(request.getSessionExamId())
                    .studentId(studentId)
                    .isDeleted(false)
                    .build();
            notificationService.createNotificationForUser(currentUser,entity.getStudentId(), NotificationObjectType.EXAM_JOINED,sessionExam.getSessionExamId(),sessionExam);
            StudentSessionExam saved = studentSessionExamRepository.saveAndFlush(entity);
            
            // Gửi email thông báo khi lần đầu giao bài thi cho sinh viên
            try {
                // Kiểm tra ClassroomSetting có bật notifyEmail không
                ClassroomSetting setting = classroomSettingRepository.findByClassroomId(sessionExam.getClassId())
                        .orElse(null);
                
                if (setting != null && setting.getNotifyEmail() && classMember.getUser().getEmail() != null) {
//                    emailService.sendExamEmail(sessionExam, classMember.getUser(), frontendUrl);
                }
            } catch (Exception e) {
                log.error("Failed to send exam assignment email to student {}: {}", studentId, e.getMessage());
            }
            
            responses.add(mapToResponse(saved, classMember.getUser()));
        }

        log.info("Added {} students to session exam {}", responses.size(), request.getSessionExamId());
        return responses;
    }

    @Override
    @Transactional
    public void removeStudent(Long sessionExamId, Long studentId) {
        log.info("Removing student from session exam");
        User currentUser = authService.getCurrentUser();

        // Validate session exam exists and belongs to teacher
        sessionExamRepository.findBySessionExamIdAndCreatedByAndIsDeletedFalse(sessionExamId,currentUser.getId())
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));

        // Find student session exam
        StudentSessionExam studentSessionExam = studentSessionExamRepository
                .findBySessionExamIdAndStudentIdAndIsDeletedFalse(sessionExamId, studentId)
                .orElseThrow(() -> new AppException(AppConst.MessageConst.NOT_FOUND,
                        messageUtils.getMessage(AppConst.MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        
        ExamSubmissionStatus submissionStatus = studentSessionExam.getSubmissionStatus();
        boolean hasParticipated = studentSessionExam.getExamStartTime() != null 
                || studentSessionExam.getJoinedAt() != null
                || (submissionStatus != null && submissionStatus != ExamSubmissionStatus.NOT_STARTED);
        boolean hasSubmitted = studentSessionExam.getSubmissionTime() != null
                || (submissionStatus != null && submissionStatus == ExamSubmissionStatus.SUBMITTED);
        
        if (hasParticipated || hasSubmitted) {
            throw new AppException(AppConst.MessageConst.CANNOT_REMOVE_STUDENT_EXAM_PARTICIPATED,
                    messageUtils.getMessage(AppConst.MessageConst.CANNOT_REMOVE_STUDENT_EXAM_PARTICIPATED), 
                    HttpStatus.BAD_REQUEST);
        }
        
        studentSessionExamRepository.delete(studentSessionExam);
    }

    private StudentSessionExamResponse mapToResponse(StudentSessionExam entity, User student) {
        StudentSessionExamResponse response = new StudentSessionExamResponse();
        response.setStudentSessionExamId(entity.getStudentSessionExamId());
        response.setSessionExamId(entity.getSessionExamId());
        response.setStudentId(entity.getStudentId());
        response.setStudentFullName(student.getFullName());
        response.setStudentUsername(student.getUsername());
        response.setStudentCode(student.getCode());
        response.setStudentEmail(student.getEmail());
        response.setStudentAvatarUrl(student.getAvatarUrl());
        return response;
    }
}

