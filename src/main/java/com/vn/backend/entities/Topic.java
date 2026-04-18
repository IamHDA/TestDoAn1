package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long topicId;

    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;


    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    private Boolean isActive;

    @Column(name = "prerequisite_topic_id", nullable = true)
    private Long prerequisiteTopicId;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "prerequisite_topic_id",
            referencedColumnName = "topicId",
            nullable = true,
            insertable = false,
            updatable = false
    )
    private Topic prerequisiteTopic;
}
