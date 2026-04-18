package com.vn.backend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "exam_question_answer_snapshots")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionAnswerSnapshot extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "source_answer_id", nullable = false)
  private Long sourceAnswerId;

  @Column(name = "answer_content", nullable = false, columnDefinition = "TEXT")
  private String answerContent;

  @Column(name = "is_correct", nullable = false)
  private Boolean isCorrect;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_question_snapshot_id", referencedColumnName = "id")
  private ExamQuestionSnapshot examQuestionSnapshot;

}
