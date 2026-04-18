package com.vn.backend.entities;

import com.vn.backend.entities.classid.ExamQuestionId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_question")
@IdClass(ExamQuestionId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion extends BaseEntity {

    @Id
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Id
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "orderIndex")
    private Integer orderIndex;

    @Column(name = "score")
    private Double score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", insertable = false, updatable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", insertable = false, updatable = false)
    private Question question;
}
