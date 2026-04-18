package com.vn.backend.scheduler;

public interface FlexExamSchedulerService {
    /**
     * Scheduler job chạy mỗi 1 phút để:
     * 1. Lưu bài làm từ Redis vào DB (submissionResult)
     * 2. Auto-submit các bài thi FLEX đã hết thời gian
     */
    void processFlexExams();
}

