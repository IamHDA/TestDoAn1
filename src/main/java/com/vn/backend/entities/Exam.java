package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "exams")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long examId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
}
