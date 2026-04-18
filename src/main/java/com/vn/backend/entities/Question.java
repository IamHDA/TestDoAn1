package com.vn.backend.entities;

import com.vn.backend.enums.QuestionType;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "is_review_question", nullable = false)
    @Builder.Default
    private Boolean isReviewQuestion = false;

    @Column(name = "is_added_to_review", nullable = false)
    @Builder.Default
    private Boolean isAddedToReview = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", referencedColumnName = "topicId", insertable = false, updatable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    private List<Answer> answers;

}
