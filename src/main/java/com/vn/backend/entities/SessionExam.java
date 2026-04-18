package com.vn.backend.entities;

import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.QuestionOrderMode;
import com.vn.backend.enums.SessionExamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionExam extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "session_exam_id")
  private Long sessionExamId;

  @Column(name = "classroom_id", nullable = false)
  private Long classId;

  @Column(name = "start_date", nullable = false)
  private LocalDateTime startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDateTime endDate;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "duration", nullable = false)
  private Long duration; // minutes

  @Builder.Default
  @Column(name = "status", nullable = false)
  private SessionExamStatus status = SessionExamStatus.NOT_STARTED;

  @Enumerated(EnumType.STRING)
  @Column(name = "exam_mode", nullable = false, length = 20)
  private ExamMode examMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_order_mode", nullable = false, length = 20)
  private QuestionOrderMode questionOrderMode;

  @Column(name = "is_instantly_result", nullable = false)
  private Boolean isInstantlyResult;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "exam_id", nullable = false)
  private Long examId;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
  private User createdByUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id", referencedColumnName = "classroom_id", insertable = false, updatable = false)
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", referencedColumnName = "exam_id", insertable = false, updatable = false)
  private Exam exam;

  @Column(name = "start_scheduler")
  @Builder.Default
  private Boolean startScheduler = false;

  @Column(name = "end_scheduler")
  @Builder.Default
  private Boolean endScheduler = false;

  @Column(name = "start_scheduler_time")
  private LocalDateTime startSchedulerTime;

  @Column(name = "end_scheduler_time")
  private LocalDateTime endSchedulerTime;
}

