package com.vn.backend.entities;

import com.vn.backend.enums.ExamSubmissionStatus;
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
@Table(name = "student_session_exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSessionExam extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "student_session_exam_id")
  private Long studentSessionExamId;

  @Column(name = "session_exam_id", nullable = false)
  private Long sessionExamId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "score")
  private Double score;

  @Enumerated(EnumType.STRING)
  @Column(name = "submission_status", nullable = false)
  @Builder.Default
  private ExamSubmissionStatus submissionStatus = ExamSubmissionStatus.NOT_STARTED;

  @Column(name = "exam_start_time")
  private LocalDateTime examStartTime;

  @Column(name = "submission_time")
  private LocalDateTime submissionTime;

  @Column(name = "submission_result", columnDefinition = "TEXT")
  private String submissionResult; // JSON string containing questions and student answers

  @Column(name = "joined_at")
  private LocalDateTime joinedAt;

  @Column(name = "downloaded_at")
  private LocalDateTime downloadedAt;

  @Column(name = "violation_count")
  @Builder.Default
  private Integer violationCount = 0;

  @Column(name = "correct_count")
  private Integer correctCount;

  @Column(name = "total_questions")
  private Integer totalQuestions;

  @Column(name = "auto_graded")
  @Builder.Default
  private Boolean autoGraded = false;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_exam_id", referencedColumnName = "session_exam_id",
      insertable = false, updatable = false)
  private SessionExam sessionExam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", referencedColumnName = "id",
      insertable = false, updatable = false)
  private User student;
}

