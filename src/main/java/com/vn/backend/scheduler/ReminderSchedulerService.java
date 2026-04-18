package com.vn.backend.scheduler;

public interface ReminderSchedulerService {

  /**
   * Job chạy mỗi 0 giờ tối mỗi ngày để nhắc nhở bài tập/bài thi chưa nộp đã đến hạn
   */
  void sendReminderEmails();
}

