package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // Nội dung đáp án

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect; // Có phải đáp án đúng không

    @Column(name = "display_order")
    private Integer displayOrder; // Thứ tự hiển thị cho đáp án

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ==== Relationships ====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    private Question question;
}
