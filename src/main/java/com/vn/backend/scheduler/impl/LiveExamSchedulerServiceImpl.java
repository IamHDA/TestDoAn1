package com.vn.backend.scheduler.impl;

import com.vn.backend.constants.AppConst;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.repositories.SessionExamRepository;
import com.vn.backend.scheduler.LiveExamSchedulerService;
import com.vn.backend.services.SessionExamService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveExamSchedulerServiceImpl implements LiveExamSchedulerService {

  private final SessionExamRepository sessionExamRepository;
  private final SessionExamService sessionExamService;

  @Override
  @Async("examTaskExecutor")
  @Scheduled(cron = "0 * * * * *")
  public void startLiveSessionExam() {
    try {
      log.info("Scheduler started live session");
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime initWindow = now.plusMinutes(AppConst.COUNTDOWN_M);
      List<SessionExam> sessionExams = sessionExamRepository.findLiveSessionExamsToInit(
          now, initWindow);

      for (SessionExam sessionExam : sessionExams) {
        try {
          sessionExamService.startLiveSessionExam(sessionExam);
          log.info("Scheduler started live session exam: {}", sessionExam.getSessionExamId());
        } catch (Exception e) {
          log.error("Scheduler started live session exam failed: {}",
              sessionExam.getSessionExamId(), e);
        }
      }
    } catch (Exception e) {
      log.error("Scheduler started live session exam error", e);
    }
  }

  @Override
  @Async("examTaskExecutor")
  @Scheduled(cron = "0 * * * * *")
  public void endLiveSessionExam() {
    try {
      log.info("Scheduler end live session");
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime windowStart = now.minusMinutes(1);
      List<SessionExam> sessionExams = sessionExamRepository.findEndingLiveSessionExams(
          now, windowStart);

      for (SessionExam sessionExam : sessionExams) {
        try {
          sessionExamService.endLiveSessionExam(sessionExam);
          log.info("Scheduler end live session exam: {}", sessionExam.getSessionExamId());
        } catch (Exception e) {
          log.error("Scheduler end live session exam failed: {}", sessionExam.getSessionExamId(),
              e);
        }
      }
    } catch (Exception e) {
      log.error("Scheduler end live session exam error", e);
    }
  }
}
