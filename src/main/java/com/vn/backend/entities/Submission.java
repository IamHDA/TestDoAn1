package com.vn.backend.entities;

import com.vn.backend.enums.GradingStatus;
import com.vn.backend.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "submissions")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", insertable = false, updatable = false)
    private Assignment assignment;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User student;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "grade")
    private Double grade;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false)
    private SubmissionStatus submissionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false)
    private GradingStatus gradingStatus;
}
