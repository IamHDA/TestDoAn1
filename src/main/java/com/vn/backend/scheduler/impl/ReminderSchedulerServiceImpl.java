package com.vn.backend.scheduler.impl;

import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.entities.ClassroomSetting;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.Submission;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.ExamSubmissionStatus;
import com.vn.backend.enums.SubmissionStatus;
import com.vn.backend.repositories.AssignmentRepository;
import com.vn.backend.repositories.ClassMemberRepository;
import com.vn.backend.repositories.ClassroomSettingRepository;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.repositories.StudentSessionExamRepository;
import com.vn.backend.repositories.SubmissionRepository;
import com.vn.backend.repositories.UserRepository;
import com.vn.backend.scheduler.ReminderSchedulerService;
import com.vn.backend.services.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerServiceImpl implements ReminderSchedulerService {

  private final AssignmentRepository assignmentRepository;
  private final SessionExamRepository sessionExamRepository;
  private final SubmissionRepository submissionRepository;
  private final StudentSessionExamRepository studentSessionExamRepository;
  private final ClassMemberRepository classMemberRepository;
  private final ClassroomSettingRepository classroomSettingRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;

  @Value("${frontend.url:http://localhost:3000}")
  private String frontendUrl;

  @Override
  @Async("examTaskExecutor")
  @Scheduled(cron = "0 0 0 * * *") // Chạy mỗi 0 giờ tối (00:00:00) mỗi ngày
  @Transactional(readOnly = true)
  public void sendReminderEmails() {
    try {
      log.info("Starting reminder email scheduler job");
      LocalDateTime now = LocalDateTime.now();

      // Xử lý nhắc nhở bài tập
      sendAssignmentReminders(now);

      // Xử lý nhắc nhở bài thi
      sendExamReminders(now);

      log.info("Completed reminder email scheduler job");
    } catch (Exception e) {
      log.error("Error in reminder email scheduler job", e);
    }
  }

  private void sendAssignmentReminders(LocalDateTime now) {
    // Lấy tất cả assignments đã quá hạn
    List<Assignment> overdueAssignments = assignmentRepository.findAll().stream()
        .filter(a -> !a.isDeleted())
        .filter(a -> a.getDueDate() != null && a.getDueDate().isBefore(now))
        .collect(Collectors.toList());

    for (Assignment assignment : overdueAssignments) {
      try {
        // Kiểm tra ClassroomSetting có bật notifyEmail không
        ClassroomSetting setting = classroomSettingRepository.findByClassroomId(assignment.getClassroomId())
            .orElse(null);
        
        if (setting == null || !setting.getNotifyEmail()) {
          log.debug("Email notification disabled for classroom: {}", assignment.getClassroomId());
          continue;
        }

        // Lấy tất cả sinh viên trong lớp
        List<ClassMember> students = classMemberRepository
            .findByClassroomIdAndMemberRoleAndMemberStatus(
                assignment.getClassroomId(),
                ClassMemberRole.STUDENT,
                ClassMemberStatus.ACTIVE);

        for (ClassMember member : students) {
          User student = userRepository.findById(member.getUserId()).orElse(null);
          if (student == null || student.getEmail() == null) {
            continue;
          }

          // Kiểm tra sinh viên đã nộp bài chưa
          Submission submission = submissionRepository
              .findByAssignmentIdAndStudentId(assignment.getAssignmentId(), student.getId())
              .orElse(null);

          if (submission != null && 
              (submission.getSubmissionStatus() == SubmissionStatus.SUBMITTED ||
               submission.getSubmissionStatus() == SubmissionStatus.LATE_SUBMITTED)) {
            // Đã nộp rồi, không gửi nhắc nhở
            continue;
          }

          // Tính số ngày quá hạn
          long daysOverdue = DAYS.between(assignment.getDueDate(), now);
          
          // Chỉ gửi nhắc nhở khi quá hạn 1 ngày hoặc 3 ngày
          if (daysOverdue == 1 || daysOverdue == 3) {
            emailService.sendReminderEmail(student, assignment, (int) daysOverdue, frontendUrl);
            log.info("Sent reminder email for assignment {} to student {} ({} days overdue)",
                assignment.getAssignmentId(), student.getEmail(), daysOverdue);
          }
        }
      } catch (Exception e) {
        log.error("Error sending assignment reminders for assignment {}: {}",
            assignment.getAssignmentId(), e.getMessage(), e);
      }
    }
  }

  private void sendExamReminders(LocalDateTime now) {
    // Lấy tất cả session exams đã quá hạn
    List<SessionExam> overdueExams = sessionExamRepository.findAll().stream()
        .filter(se -> !se.getIsDeleted())
        .filter(se -> se.getEndDate() != null && se.getEndDate().isBefore(now))
        .collect(Collectors.toList());

    for (SessionExam sessionExam : overdueExams) {
      try {
        // Kiểm tra ClassroomSetting có bật notifyEmail không
        ClassroomSetting setting = classroomSettingRepository.findByClassroomId(sessionExam.getClassId())
            .orElse(null);
        
        if (setting == null || !setting.getNotifyEmail()) {
          log.debug("Email notification disabled for classroom: {}", sessionExam.getClassId());
          continue;
        }

        // Lấy tất cả sinh viên được assign vào bài thi
        List<StudentSessionExam> studentExams = studentSessionExamRepository
            .findAllBySessionExamId(sessionExam.getSessionExamId());

        Set<Long> studentIds = studentExams.stream()
            .map(StudentSessionExam::getStudentId)
            .collect(Collectors.toSet());

        for (Long studentId : studentIds) {
          User student = userRepository.findById(studentId).orElse(null);
          if (student == null || student.getEmail() == null) {
            continue;
          }

          // Kiểm tra sinh viên đã nộp bài chưa
          StudentSessionExam studentExam = studentExams.stream()
              .filter(se -> se.getStudentId().equals(studentId))
              .findFirst()
              .orElse(null);

          if (studentExam != null && 
              studentExam.getSubmissionStatus() == ExamSubmissionStatus.SUBMITTED) {
            // Đã nộp rồi, không gửi nhắc nhở
            continue;
          }

          // Tính số ngày quá hạn
          long daysOverdue = DAYS.between(sessionExam.getEndDate(), now);
          
          // Chỉ gửi nhắc nhở khi quá hạn 1 ngày hoặc 3 ngày
          if (daysOverdue == 1 || daysOverdue == 3) {
            emailService.sendExamReminderEmail(student, sessionExam, (int) daysOverdue, frontendUrl);
            log.info("Sent reminder email for exam {} to student {} ({} days overdue)",
                sessionExam.getSessionExamId(), student.getEmail(), daysOverdue);
          }
        }
      } catch (Exception e) {
        log.error("Error sending exam reminders for exam {}: {}",
            sessionExam.getSessionExamId(), e.getMessage(), e);
      }
    }
  }
}

