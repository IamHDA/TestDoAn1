package com.vn.backend.entities;

import com.vn.backend.enums.QuestionType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "exam_question_snapshot")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionSnapshot extends BaseEntity{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "exam_id", nullable = false)
  private Long examId;

  @Column(name = "session_exam_id")
  private Long sessionExamId;

  @Column(name = "source_question_id", nullable = false)
  private Long sourceQuestionId;

  @Column(name = "question_content", nullable = false, columnDefinition = "TEXT")
  private String questionContent;

  @Column(name = "question_image_url")
  private String questionImageUrl;

  @Column(name = "question_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private QuestionType questionType;

  @Column(name = "difficulty_level", nullable = false)
  private Integer difficultyLevel;

  @Column(name = "topic_id")
  private Long topicId;

  @Column(name = "score")
  private Double score;

  @Column(name = "orderIndex")
  private Integer orderIndex;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", insertable = false, updatable = false)
  private Exam exam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_exam_id", insertable = false, updatable = false)
  private SessionExam sessionExam;

  @OneToMany(
      mappedBy = "examQuestionSnapshot",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<ExamQuestionAnswerSnapshot> examQuestionAnswers;
}
