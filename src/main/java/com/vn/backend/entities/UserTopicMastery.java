package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_topic_mastery", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTopicMastery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_topic_mastery_id")
    private Long userTopicMasteryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "mastery_score", nullable = false)
    @Builder.Default
    private Double masteryScore = 0.0; // Điểm mastery từ 0.0 đến 1.0

    @Column(name = "total_attempts", nullable = false)
    @Builder.Default
    private Integer totalAttempts = 0; // Tổng số lần làm bài

    @Column(name = "correct_attempts", nullable = false)
    @Builder.Default
    private Integer correctAttempts = 0; // Số lần làm đúng

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate; // Ngày cần ôn tập lại (cho spaced repetition)

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", referencedColumnName = "topicId", insertable = false, updatable = false)
    private Topic topic;
}

