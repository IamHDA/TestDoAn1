package com.vn.backend.services.impl;

import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.dto.request.attachment.AttachmentCreateRequestDTO;
import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.submission.*;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.dto.response.common.ResponseListData;
import com.vn.backend.dto.response.submission.*;
import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.Attachment;
import com.vn.backend.entities.Submission;
import com.vn.backend.entities.User;
import com.vn.backend.enums.*;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.repositories.AssignmentRepository;
import com.vn.backend.repositories.AttachmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.SubmissionRepository;
import com.vn.backend.services.AuthService;
import com.vn.backend.services.FileService;
import com.vn.backend.services.SubmissionService;
import com.vn.backend.utils.DateUtils;
import com.vn.backend.utils.ExcelUtils;
import com.vn.backend.utils.FileUtils;
import com.vn.backend.utils.MessageUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SubmissionServiceImpl extends BaseService implements SubmissionService {

    public static final String[] EXCEL_HEADERS = {
            "STT",
            "Username",
            "Họ tên",
            "Mã sinh viên",
            "Trạng thái nộp bài",
            "Trạng thái chấm điểm",
            "Thời gian nộp",
            "Điểm"
    };
    public static final String[] EXCEL_FIELD_NAMES = {
            "index",
            "username",
            "fullName",
            "code",
            "submissionStatus",
            "gradingStatus",
            "submittedAt",
            "grade"
    };
    private final AuthService authService;
    private final SubmissionRepository submissionRepository;
    private final AttachmentRepository attachmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassMemberRepository classMemberRepository;
    private final FileService fileService;

    public SubmissionServiceImpl(MessageUtils messageUtils, AuthService authService, SubmissionRepository submissionRepository, AttachmentRepository attachmentRepository, AssignmentRepository assignmentRepository, ClassMemberRepository classMemberRepository, FileService fileService) {
        super(messageUtils);
        this.authService = authService;
        this.submissionRepository = submissionRepository;
        this.attachmentRepository = attachmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.classMemberRepository = classMemberRepository;
        this.fileService = fileService;
    }

    @Override
    public SubmissionDetailResponse getDetailSubmission(String submissionId) {
        log.info("Start service to get detail submission");

        User user = authService.getCurrentUser();
        if (!submissionRepository.hasPermission(Long.parseLong(submissionId), user.getId())) {
            throw new AppException(MessageConst.FORBIDDEN, messageUtils.getMessage(MessageConst.FORBIDDEN), HttpStatus.FORBIDDEN);
        }
        Submission submission = submissionRepository.findById(Long.parseLong(submissionId))
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
        List<Attachment> attachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(submission.getSubmissionId(), AttachmentType.SUBMISSION);
        SubmissionDetailResponse submissionDetailResponse = SubmissionDetailResponse.fromEntity(submission, attachments);

        log.info("End service get detail submission");
        return submissionDetailResponse;
    }

    @Override
    public SubmissionDetailResponse getMySubmission(String assignmentId) {
        log.info("Start service to get my submission");

        User user = authService.getCurrentUser();
        Optional<Submission> submissionOtp = submissionRepository.findByAssignmentIdAndStudentId(
                Long.parseLong(assignmentId), user.getId()
        );
        if (submissionOtp.isEmpty()) {
            log.info("End service get my submission");
            return SubmissionDetailResponse.builder().build();
        }
        List<Attachment> attachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(submissionOtp.get().getSubmissionId(), AttachmentType.SUBMISSION);
        SubmissionDetailResponse response = SubmissionDetailResponse.fromEntity(submissionOtp.get(), attachments);
        log.info("End service get my submission");

        return response;
    }

    @Transactional
    @Override
    public void createDefaultSubmissions(Assignment assignment) {
        if (assignment == null) {
            log.error("Create default submissions fail");
            return;
        }
        Set<Long> studentIds = classMemberRepository.getClassMemberIdsActive(assignment.getClassroomId(), ClassMemberRole.STUDENT);
        for (Long studentId : studentIds) {
            Submission submission = Submission.builder()
                    .assignmentId(assignment.getAssignmentId())
                    .studentId(studentId)
                    .submissionStatus(SubmissionStatus.NOT_SUBMITTED)
                    .gradingStatus(GradingStatus.NOT_GRADED)
                    .build();
            submissionRepository.save(submission);
        }
    }

    @Override
    @Transactional
    public void deleteAttachmentInSubmission(String attachmentId) {
        log.info("Start service to delete attachment in submission");

        User user = authService.getCurrentUser();

        Attachment attachment = attachmentRepository.findByAttachmentIdAndUploadedByAndIsDeletedEquals(Long.parseLong(attachmentId), user.getId(), false)
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        Submission submission = submissionRepository.findBySubmissionIdAndStudentId(attachment.getObjectId(), user.getId())
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        Assignment assignment = this.findAssignmentForStudentActive(user.getId(), submission.getAssignmentId());

        // Nếu không được phép nộp muộn
        boolean isLate = isSubmissionLate(assignment);
        if (isLate && assignment.isSubmissionClosed()) {
            throw new AppException(MessageConst.LATE_SUBMISSION_NOT_ALLOWED, messageUtils.getMessage(MessageConst.LATE_SUBMISSION_NOT_ALLOWED), HttpStatus.BAD_REQUEST);
        }
        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);

        List<Attachment> attachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(submission.getSubmissionId(), AttachmentType.SUBMISSION);
        if (attachments.isEmpty()) {
            submission.setSubmittedAt(null);
            submission.setSubmissionStatus(SubmissionStatus.NOT_SUBMITTED);
        } else if (isLate) {
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setSubmissionStatus(SubmissionStatus.LATE_SUBMITTED);
        }
        submissionRepository.save(submission);

        log.info("End service delete attachment in submission");
    }

    @Override
    @Transactional
    public void addAttachmentToSubmission(String submissionId, SubmissionUpdateRequest request) {
        log.info("Start service to update submission");

        User user = authService.getCurrentUser();
        SubmissionUpdateRequestDTO dto = request.toDTO();

        Submission submission = submissionRepository.findBySubmissionIdAndStudentId(Long.parseLong(submissionId), user.getId())
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        Assignment assignment = this.findAssignmentForStudentActive(user.getId(), submission.getAssignmentId());
        boolean isLate = isSubmissionLate(assignment);
        if (isLate && assignment.isSubmissionClosed()) {
            throw new AppException(MessageConst.LATE_SUBMISSION_NOT_ALLOWED, messageUtils.getMessage(MessageConst.LATE_SUBMISSION_NOT_ALLOWED), HttpStatus.BAD_REQUEST);
        }
        for (AttachmentCreateRequestDTO attachmentDTO : dto.getAttachmentCreateRequestDTOList()) {
            this.createAttachment(user.getId(), submission.getSubmissionId(), attachmentDTO);
        }
        if (isLate) {
            submission.setSubmissionStatus(SubmissionStatus.LATE_SUBMITTED);
        } else {
            submission.setSubmissionStatus(SubmissionStatus.SUBMITTED);
        }
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        log.info("End service add attachment to submission");
    }

    @Override
    public ResponseListData<SubmissionSearchResponse> searchSubmission(BaseFilterSearchRequest<SubmissionSearchRequest> request) {
        log.info("Start service to search submission");

        User user = authService.getCurrentUser();
        SubmissionSearchRequestDTO dto = request.getFilters().toDTO();
        if (!Boolean.TRUE.equals(assignmentRepository.canUserViewSubmissions(dto.getAssignmentId(), user.getId()))) {
            throw new AppException(MessageConst.FORBIDDEN,
                    messageUtils.getMessage(MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN);
        }
        Pageable pageable = request.getPagination().getPagingMeta().toPageable();
        Page<SubmissionSearchQueryDTO> queryDTOS = submissionRepository.searchSubmission(dto, pageable);

        List<SubmissionSearchResponse> response = queryDTOS.stream().map(SubmissionSearchResponse::fromDTO)
                .toList();

        PagingMeta pagingMeta = request.getPagination().getPagingMeta();
        pagingMeta.setTotalRows(queryDTOS.getTotalElements());
        pagingMeta.setTotalPages(queryDTOS.getTotalPages());

        log.info("End service search submission");
        return new ResponseListData<>(response, pagingMeta);
    }

    @Override
    public void markSubmission(String submissionId, SubmissionGradeUpdateRequest request) {
        log.info("Start service to mark submission");

        User user = authService.getCurrentUser();
        Boolean hasPermission = submissionRepository.hasPermission(
                Long.parseLong(submissionId),
                user.getId()
        );

        if (!Boolean.TRUE.equals(hasPermission)) {
            throw new AppException(
                    MessageConst.FORBIDDEN,
                    messageUtils.getMessage(MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN
            );
        }
        SubmissionGradeUpdateRequestDTO dto = request.toDTO();
        Submission submission = submissionRepository.findById(Long.parseLong(submissionId))
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.BAD_REQUEST));
        submission.setGrade(dto.getGrade());
        submission.setGradingStatus(GradingStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        log.info("End service mark submission");
    }

    private Assignment findAssignmentForStudentActive(Long userId, Long assignmentId) {
        return assignmentRepository
                .findAssignmentIfUserCanSubmit(userId, assignmentId, ClassMemberRole.STUDENT, ClassMemberStatus.ACTIVE)
                .orElseThrow(() -> new AppException(MessageConst.NOT_FOUND, messageUtils.getMessage(MessageConst.NOT_FOUND), HttpStatus.NOT_FOUND));
    }

    private void createAttachment(Long userId, Long submissionId, AttachmentCreateRequestDTO dto) {
        Attachment attachment = Attachment.builder()
                .attachmentType(AttachmentType.SUBMISSION)
                .objectId(submissionId)
                .fileUrl(dto.getFileUrl())
                .fileName(dto.getFileName())
                .uploadedBy(userId)
                .build();
        attachmentRepository.save(attachment);
    }

    private boolean isSubmissionLate(Assignment assignment) {
        LocalDateTime dueDate = assignment.getDueDate();
        return dueDate != null && DateUtils.isAfter(LocalDateTime.now(), dueDate);
    }

    @Override
    public Resource downloadAllSubmissions(String assignmentId) {
        log.info("Start service to download all submissions");

        Long assId = Long.parseLong(assignmentId);
        User user = authService.getCurrentUser();

        if (!assignmentRepository.canUserViewSubmissions(assId, user.getId())) {
            throw new AppException(MessageConst.FORBIDDEN,
                    messageUtils.getMessage(MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN);
        }

        List<Submission> submissions = submissionRepository.findAllByAssignmentId(assId);
        if (submissions.isEmpty()) {
            throw new AppException(MessageConst.NOT_FOUND,
                    messageUtils.getMessage(MessageConst.NOT_FOUND),
                    HttpStatus.BAD_REQUEST);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos, StandardCharsets.UTF_8);

        boolean hasAnyFile = false;

        for (Submission submission : submissions) {
            List<Attachment> attachments = attachmentRepository.findByObjectIdAndAttachmentTypeAndNotDeleted(
                    submission.getSubmissionId(), AttachmentType.SUBMISSION);

            for (Attachment attachment : attachments) {
                String fileName = FileUtils.getFileNameFromDefaultUrl(attachment.getFileUrl());
                if (fileName == null) {
                    continue;
                }
                hasAnyFile = true;
                Resource fileResource = fileService.downloadFile(fileName);
                File file = null;
                try {
                    file = fileResource.getFile();
                    String zipEntryName = submission.getStudent().getFullName()
                            + "_" + submission.getStudent().getCode()
                            + "/" + file.getName();

                    zipOut.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(file.toPath(), zipOut);
                    zipOut.closeEntry();
                } catch (IOException e) {
                    String safeFileName =
                            (file != null) ? file.getName() : attachment.getFileName();
                    log.error("Download file {} failed", safeFileName, e);
                    throw new AppException(MessageConst.FILE_DOWNLOAD_FAILED,
                            messageUtils.getMessage(MessageConst.FILE_DOWNLOAD_FAILED),
                            HttpStatus.BAD_REQUEST);
                }
            }
        }

        if (!hasAnyFile) {
            throw new AppException(MessageConst.NOT_FOUND,
                    messageUtils.getMessage(MessageConst.NOT_FOUND),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            zipOut.close();
        } catch (IOException e) {
            throw new AppException(MessageConst.FILE_DOWNLOAD_FAILED,
                    messageUtils.getMessage(MessageConst.FILE_DOWNLOAD_FAILED),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("End service download all submissions");
        return new ByteArrayResource(baos.toByteArray());
    }

    @Override
    public ByteArrayResource downloadGradeTemplate(String assignmentId) {
        log.info("Start service to download grade template");

        Long assId = Long.parseLong(assignmentId);
        User user = authService.getCurrentUser();

        if (!assignmentRepository.canUserViewSubmissions(assId, user.getId())) {
            throw new AppException(MessageConst.FORBIDDEN,
                    messageUtils.getMessage(MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN);
        }
        List<SubmissionExcelQueryDTO> data = submissionRepository.findAllForExcel(assId);
        List<SubmissionExcelExportDTO> excelData = new ArrayList<>();
        int stt = 1;
        for (SubmissionExcelQueryDTO p : data) {
            excelData.add(SubmissionExcelExportDTO.toExcelDTO(p, stt++));
        }
        ByteArrayResource resource = ExcelUtils.exportToExcel(
                excelData,
                "DANH_SACH",
                EXCEL_HEADERS,
                EXCEL_FIELD_NAMES
        );
        log.info("End service download grade template");
        return resource;
    }

    @Override
    public void importSubmissionScoresFromExcel(String assignmentId, MultipartFile file) {
        log.info("Start service to import submission scores from excel");

        Long assId = Long.parseLong(assignmentId);
        User user = authService.getCurrentUser();

        if (!assignmentRepository.canUserViewSubmissions(assId, user.getId())) {
            throw new AppException(MessageConst.FORBIDDEN,
                    messageUtils.getMessage(MessageConst.FORBIDDEN),
                    HttpStatus.FORBIDDEN);
        }
        List<SubmissionImportDTO> importList = this.parseSubmissionExcel(file);

        List<Submission> submissions = submissionRepository.findAllByAssignmentId(assId);

        // key = studentCode
        Map<String, Submission> submissionMap = submissions.stream()
                .filter(s -> s.getStudent() != null)
                .collect(Collectors.toMap(
                        s -> s.getStudent().getCode(),
                        s -> s
                ));
        for (SubmissionImportDTO dto : importList) {
            Submission submission = submissionMap.get(dto.getCode());
            if (submission == null) {
                throw new AppException(
                        MessageConst.IMPORT_SUBMISSION_NOT_FOUND,
                        messageUtils.getMessage(MessageConst.IMPORT_SUBMISSION_NOT_FOUND, dto.getUsername(), dto.getCode()),
                        HttpStatus.NOT_FOUND
                );
            }

            if (dto.getGrade() != null) {
                submission.setGradedAt(!dto.getGrade().equals(submission.getGrade()) ? LocalDateTime.now() : submission.getGradedAt());
                submission.setGrade(dto.getGrade());
                submission.setGradingStatus(GradingStatus.GRADED);
            }
        }

        submissionRepository.saveAll(submissions);

        log.info("End service import submission scores from excel");
    }

    private List<SubmissionImportDTO> parseSubmissionExcel(MultipartFile file) {
        List<SubmissionImportDTO> results = new ArrayList<>();

        try (Workbook workbook = ExcelUtils.createWorkbook(file)) {
            Sheet sheet = ExcelUtils.getSheet(workbook, 0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = ExcelUtils.getRow(sheet, i);
                if (row == null) continue;

                String username = ExcelUtils.getCellValueAsString(row.getCell(1));
                String fullName = ExcelUtils.getCellValueAsString(row.getCell(2));
                String code = ExcelUtils.getCellValueAsString(row.getCell(3));
                String gradeStr = ExcelUtils.getCellValueAsString(row.getCell(7));

                if (isBlank(username) || isBlank(code)) {
                    log.warn("Data line {} invalid", i);
                    throw new AppException(MessageConst.IMPORT_MISSING_STUDENT_INFO, messageUtils.getMessage(MessageConst.IMPORT_MISSING_STUDENT_INFO, String.valueOf(i)), HttpStatus.BAD_REQUEST);
                }

                Double grade = null;
                if (gradeStr != null && !gradeStr.isEmpty()) {
                    try {
                        grade = Double.parseDouble(gradeStr);
                    } catch (NumberFormatException ignored) {
                        log.warn("Grade line {} invalid", i);
                        throw new AppException(MessageConst.IMPORT_INVALID_GRADE_FORMAT, messageUtils.getMessage(MessageConst.IMPORT_INVALID_GRADE_FORMAT, String.valueOf(i)), HttpStatus.BAD_REQUEST);

                    }
                }
                results.add(new SubmissionImportDTO(username, fullName, code, grade));
            }

        } catch (IOException e) {
            throw new AppException(MessageConst.FILE_UPLOAD_FAILED, messageUtils.getMessage(MessageConst.FILE_UPLOAD_FAILED), HttpStatus.BAD_REQUEST);
        }

        return results;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
