package com.vn.backend.services;

import com.vn.backend.entities.Assignment;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.entities.User;
import java.util.List;

public interface EmailService {

  /**
   * Gửi email thông báo khi giảng viên giao bài thi cho sinh viên lần đầu
   */
  void sendExamEmail(SessionExam sessionExam, User student, String frontendUrl);

  /**
   * Gửi email thông báo khi tạo bài tập mới
   */
  void sendAssignmentCreatedEmail(Assignment assignment, List<User> students, String frontendUrl);

  /**
   * Gửi email nhắc nhở bài tập/bài thi chưa nộp đã đến hạn
   */
  void sendReminderEmail(User student, Assignment assignment, int daysOverdue, String frontendUrl);

  /**
   * Gửi email nhắc nhở bài thi chưa nộp đã đến hạn
   */
  void sendExamReminderEmail(User student, SessionExam sessionExam, int daysOverdue, String frontendUrl);
}

