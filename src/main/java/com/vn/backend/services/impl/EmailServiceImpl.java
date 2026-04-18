package com.vn.backend.services.impl;

import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username:no-reply@classsystem.com}")
  private String fromEmail;

  @Value("${frontend.url:http://localhost:3000}")
  private String frontendUrl;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Override
  public void sendExamEmail(SessionExam sessionExam, User student, String frontendUrl) {
    String classroomName = sessionExam.getClassroom() != null ? sessionExam.getClassroom().getClassName() : "Lớp học";
    String examUrl = frontendUrl + "/classes/" + sessionExam.getClassId() + "?tab=exams";

      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(student.getEmail());
        helper.setSubject("Bài thi mới: " + sessionExam.getTitle());

        String htmlContent = buildExamEmailHtml(
            student.getFullName(),
            classroomName,
            sessionExam.getTitle(),
            sessionExam.getStartDate().format(DATE_FORMATTER),
            sessionExam.getEndDate().format(DATE_FORMATTER),
            sessionExam.getDuration(),
            examUrl
        );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        log.info("Sent exam assignment email to student: {}", student.getEmail());
      } catch (MessagingException e) {
        log.error("Failed to send exam assignment email to student {}: {}", student.getEmail(), e.getMessage());
      }

  }

  @Override
  public void sendAssignmentCreatedEmail(Assignment assignment, List<User> students, String frontendUrl) {
    String classroomName = assignment.getClassroom() != null ? assignment.getClassroom().getClassName() : "Lớp học";
    String assignmentUrl = frontendUrl + "/classes/" + assignment.getClassroomId() + "/assignments/" + assignment.getAssignmentId();
    String dueDateStr = assignment.getDueDate() != null ? assignment.getDueDate().format(DATE_FORMATTER) : "Chưa có hạn nộp";
    
    for (User student : students) {
      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(student.getEmail());
        helper.setSubject("Bài tập mới: " + assignment.getTitle());

        String htmlContent = buildAssignmentCreatedEmailHtml(
            student.getFullName(),
            classroomName,
            assignment.getTitle(),
            dueDateStr,
            assignmentUrl
        );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        log.info("Sent assignment created email to student: {}", student.getEmail());
      } catch (MessagingException e) {
        log.error("Failed to send assignment created email to student {}: {}", student.getEmail(), e.getMessage());
      }
    }
  }

  @Override
  public void sendReminderEmail(User student, Assignment assignment, int daysOverdue, String frontendUrl) {
    String classroomName = assignment.getClassroom() != null ? assignment.getClassroom().getClassName() : "Lớp học";
    String assignmentUrl = frontendUrl + "/classroom/" + assignment.getClassroomId() + "/assignment/" + assignment.getAssignmentId();
    String dueDateStr = assignment.getDueDate() != null ? assignment.getDueDate().format(DATE_FORMATTER) : "Chưa có hạn nộp";
    
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(student.getEmail());
      helper.setSubject("Nhắc nhở: Bài tập \"" + assignment.getTitle() + "\" đã đến hạn");

      String htmlContent = buildReminderEmailHtml(
          student.getFullName(),
          classroomName,
          assignment.getTitle(),
          dueDateStr,
          daysOverdue,
          assignmentUrl,
          "bài tập"
      );

      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("Sent reminder email to student: {} for assignment: {}", student.getEmail(), assignment.getTitle());
    } catch (MessagingException e) {
      log.error("Failed to send reminder email to student {}: {}", student.getEmail(), e.getMessage());
    }
  }

  @Override
  public void sendExamReminderEmail(User student, SessionExam sessionExam, int daysOverdue, String frontendUrl) {
    String classroomName = sessionExam.getClassroom() != null ? sessionExam.getClassroom().getClassName() : "Lớp học";
    String examUrl = frontendUrl + "/classroom/" + sessionExam.getClassId() + "/exam/" + sessionExam.getSessionExamId();
    String endDateStr = sessionExam.getEndDate().format(DATE_FORMATTER);
    
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(student.getEmail());
      helper.setSubject("Nhắc nhở: Bài thi \"" + sessionExam.getTitle() + "\" đã đến hạn");

      String htmlContent = buildReminderEmailHtml(
          student.getFullName(),
          classroomName,
          sessionExam.getTitle(),
          endDateStr,
          daysOverdue,
          examUrl,
          "bài thi"
      );

      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("Sent exam reminder email to student: {} for exam: {}", student.getEmail(), sessionExam.getTitle());
    } catch (MessagingException e) {
      log.error("Failed to send exam reminder email to student {}: {}", student.getEmail(), e.getMessage());
    }
  }

  private String buildExamEmailHtml(String studentName, String classroomName, String examTitle,
      String startDate, String endDate, Long duration, String examUrl) {
    return """ 
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Bài thi mới</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#333;">
<table width="100%%" cellpadding="0" cellspacing="0">
<tr>
<td align="center" style="padding:30px 10px;">
<table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);">

<tr>
<td style="background:linear-gradient(135deg,#4CAF50,#43A047);padding:25px;text-align:center;color:#fff;">
<h1 style="margin:0;font-size:26px;">Bài thi mới</h1>
<p style="margin:8px 0 0;font-size:14px;opacity:0.9;">
Thông báo từ hệ thống quản lý lớp học
</p>
</td>
</tr>

<tr>
<td style="padding:30px;">
<p style="font-size:16px;margin:0 0 15px;">
Xin chào <strong>%s</strong>,
</p>

<p style="margin:0 0 20px;font-size:15px;">
Giảng viên đã giao một <strong>bài thi mới</strong> trong lớp
<strong>%s</strong>.
</p>

<table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f8f4;border-left:5px solid #4CAF50;border-radius:6px;margin-bottom:25px;">
<tr>
<td style="padding:20px;">
<h3 style="margin:0 0 10px;color:#2e7d32;">
%s
</h3>
<p style="margin:6px 0;font-size:14px;">
<strong>Bắt đầu:</strong> %s
</p>
<p style="margin:6px 0;font-size:14px;">
<strong>Kết thúc:</strong> %s
</p>
<p style="margin:6px 0;font-size:14px;">
<strong>Thời gian làm bài:</strong> %s phút
</p>
</td>
</tr>
</table>

<p style="font-size:14px;margin-bottom:25px;color:#555;">
Vui lòng truy cập bài thi và hoàn thành trong thời gian quy định.
</p>

<div style="text-align:center;">
<a href="%s" style="display:inline-block;padding:14px 36px;background:#4CAF50;color:#ffffff;text-decoration:none;font-size:15px;font-weight:bold;border-radius:30px;">
Xem bài thi
</a>
</div>
</td>
</tr>

<tr>
<td style="background:#fafafa;padding:20px;text-align:center;font-size:12px;color:#777;">
<p style="margin:0;">
Email này được gửi tự động. Vui lòng không trả lời.
</p>
<p style="margin:6px 0 0;">
© 2025 Hệ thống quản lý lớp học
</p>
</td>
</tr>

</table>
</td>
</tr>
</table>
</body>
</html>""".formatted(studentName, classroomName, examTitle, startDate, endDate, duration, examUrl);
  }

  private String buildAssignmentCreatedEmailHtml(String studentName, String classroomName, String assignmentTitle,
      String dueDate, String assignmentUrl) {
    return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Bài tập mới</title>
            </head>
            
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#333;">
            <table width="100%%" cellpadding="0" cellspacing="0">
            <tr>
            <td align="center" style="padding:30px 10px;">
            
            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 6px 18px rgba(0,0,0,0.08);">
            
            <!-- Header -->
            <tr>
            <td style="background:linear-gradient(135deg,#2196F3,#1e88e5);padding:28px;text-align:center;color:#ffffff;">
            <h1 style="margin:0;font-size:26px;"> Bài tập mới</h1>
            <p style="margin:8px 0 0;font-size:14px;opacity:0.9;">
            Thông báo từ hệ thống quản lý lớp học
            </p>
            </td>
            </tr>
            
            <!-- Content -->
            <tr>
            <td style="padding:30px;">
            <p style="font-size:16px;margin:0 0 15px;">
            Xin chào <strong>%s</strong>,
            </p>
            
            <p style="margin:0 0 20px;font-size:15px;">
            Giảng viên đã đăng một <strong>bài tập mới</strong> trong lớp
            <strong>%s</strong>.
            </p>
            
            <!-- Info box -->
            <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f7fe;border-left:5px solid #2196F3;border-radius:6px;margin-bottom:25px;">
            <tr>
            <td style="padding:20px;">
            <h3 style="margin:0 0 10px;color:#1565c0;font-size:18px;">
            %s
            </h3>
            <p style="margin:6px 0;font-size:14px;">
            <strong> Hạn nộp:</strong> %s
            </p>
            </td>
            </tr>
            </table>
            
            <p style="font-size:14px;margin-bottom:25px;color:#555;">
            Vui lòng truy cập vào bài tập để xem chi tiết và nộp bài đúng hạn.
            </p>
            
            <!-- Button -->
            <div style="text-align:center;">
            <a href="%s"
               style="display:inline-block;padding:14px 36px;
                      background:#2196F3;color:#ffffff;
                      text-decoration:none;font-size:15px;
                      font-weight:bold;border-radius:30px;">
            Xem bài tập
            </a>
            </div>
            </td>
            </tr>
            
            <!-- Footer -->
            <tr>
            <td style="background:#fafafa;padding:20px;text-align:center;font-size:12px;color:#777;">
            <p style="margin:0;">
            Email này được gửi tự động từ hệ thống quản lý lớp học.
            </p>
            <p style="margin:6px 0 0;">
            © 2025 LMS System
            </p>
            </td>
            </tr>
            
            </table>
            </td>
            </tr>
            </table>
            </body>
            </html>
            
        """.formatted(studentName, classroomName, assignmentTitle, dueDate, assignmentUrl);
  }

  private String buildReminderEmailHtml(String studentName, String classroomName, String itemTitle,
      String dueDate, int daysOverdue, String itemUrl, String itemType) {
    String daysText = daysOverdue == 1 ? "1 ngày" : daysOverdue + " ngày";
    String color = daysOverdue >= 3 ? "#f44336" : "#ff9800";
    
    return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nhắc nhở quá hạn</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#333;">
            <table width="100%%" cellpadding="0" cellspacing="0">
            <tr>
            <td align="center" style="padding:30px 10px;">
            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 6px 18px rgba(0,0,0,0.08);">
            
            <!-- Header -->
            <tr>
            <td style="background:%s;padding:28px;text-align:center;color:#ffffff;">
            <h1 style="margin:0;font-size:26px;"> Nhắc nhở quá hạn</h1>
            <p style="margin:8px 0 0;font-size:14px;opacity:0.9;">
            %s đã đến hạn
            </p>
            </td>
            </tr>
            
            <!-- Content -->
            <tr>
            <td style="padding:30px;">
            <p style="font-size:16px;margin:0 0 15px;">
            Xin chào <strong>%s</strong>,
            </p>
            
            <p style="margin:0 0 20px;font-size:15px;">
            Bạn <strong>chưa nộp %s</strong> trong lớp
            <strong>%s</strong>.
            </p>
            
            <!-- Warning box -->
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#fff8e1;border-left:5px solid %s;border-radius:6px;margin-bottom:25px;">
            <tr>
            <td style="padding:20px;">
            <h3 style="margin:0 0 10px;color:#e65100;font-size:18px;">
            %s
            </h3>
            <p style="margin:6px 0;font-size:14px;">
            <strong> Hạn nộp:</strong> %s
            </p>
            <p style="margin:6px 0;font-size:14px;color:#c62828;">
            <strong> Đã quá hạn:</strong> %s
            </p>
            </td>
            </tr>
            </table>
            
            <p style="font-size:14px;margin-bottom:25px;color:#555;">
            Vui lòng truy cập vào <strong>%s</strong> để nộp bài ngay nhằm tránh ảnh hưởng đến kết quả học tập.
            </p>
            
            <!-- Button -->
            <div style="text-align:center;">
            <a href="%s"
               style="display:inline-block;padding:14px 36px;
                      background:%s;color:#ffffff;
                      text-decoration:none;font-size:15px;
                      font-weight:bold;border-radius:30px;">
             Xem %s
            </a>
            </div>
            </td>
            </tr>
            
            <!-- Footer -->
            <tr>
            <td style="background:#fafafa;padding:20px;text-align:center;font-size:12px;color:#777;">
            <p style="margin:0;">
            Email này được gửi tự động từ hệ thống quản lý lớp học.
            </p>
            <p style="margin:6px 0 0;">
            © 2025 LMS System
            </p>
            </td>
            </tr>
            
            </table>
            </td>
            </tr>
            </table>
            </body>
            </html>
            
        """.formatted(color, color, color, itemType, studentName, itemType, classroomName, itemTitle, dueDate, daysText, itemType, itemUrl, itemType);
  }
}

