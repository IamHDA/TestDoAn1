package com.vn.backend.scheduler.impl;

import com.vn.backend.scheduler.FlexExamSchedulerService;
import com.vn.backend.services.SessionExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class FlexExamSchedulerServiceImpl implements FlexExamSchedulerService {

    private final SessionExamService sessionExamService;

    public FlexExamSchedulerServiceImpl(SessionExamService sessionExamService) {
        this.sessionExamService = sessionExamService;
    }


    /**
     * Scheduler job chạy mỗi 1 phút (60000 ms)
     */
    @Scheduled(cron = "0 * * * * *")
    @Override
    @Async("examTaskExecutor")
    public void processFlexExams() {
        try {
            LocalDateTime now = LocalDateTime.now();
            sessionExamService.processFlexExamStarted(now);
            sessionExamService.processExpiredNotStartedFlexExams(now);
        } catch (Exception e) {
            log.error("Error in FlexExamScheduler job: {}", e.getMessage(), e);
        }
    }

}

