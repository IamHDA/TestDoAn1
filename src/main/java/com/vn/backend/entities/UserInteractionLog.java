package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interaction_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_log_id")
    private Long interactionLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_answer_id")
    private Long selectedAnswerId; // Đáp án user chọn (có thể null nếu chưa chọn)

    @Column(name = "was_correct", nullable = false)
    private Boolean wasCorrect; // Có trả lời đúng không

    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds; // Thời gian phản hồi (giây)

    @Column(name = "interaction_timestamp", nullable = false)
    private LocalDateTime interactionTimestamp; // Thời điểm tương tác


    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_answer_id", referencedColumnName = "answer_id", insertable = false, updatable = false)
    private Answer selectedAnswer;
}

