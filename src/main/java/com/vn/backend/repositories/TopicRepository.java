package com.vn.backend.repositories;

import com.vn.backend.entities.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    Optional<Topic> findByTopicIdAndIsDeleted(Long topicId, Boolean isDeleted);

    @Query("""
        SELECT t FROM Topic t
        WHERE t.isDeleted = false
          AND t.isActive = true
          AND (:subjectId IS NULL OR t.subjectId = :subjectId)
          AND (:searchTerm IS NULL OR :searchTerm = '' 
               OR LOWER(t.topicName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        """)
    Page<Topic> findTopicsBySubjectIdWithSearch(
            @Param("subjectId") Long subjectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );


    Optional<Topic> findByTopicIdAndIsActiveTrueAndIsDeletedFalse(Long topicId);

    @Query("""
        SELECT t.topicId, t.topicName, s.subjectName, s.subjectCode
        FROM Topic t
        LEFT JOIN Subject s ON t.subjectId = s.subjectId
        WHERE t.isDeleted = false AND t.isActive = true
        ORDER BY t.topicId ASC
        """)
    List<Object[]> listTopicsWithSubject();

    Topic findByTopicIdAndIsDeletedFalse(Long topicId);

    @Query("""
        SELECT t FROM Topic t
        WHERE t.subjectId = :subjectId
          AND t.isDeleted = false
          AND t.isActive = true
        ORDER BY t.createdAt ASC
        """)
    List<Topic> findBySubjectIdAndIsActiveTrueAndIsDeletedFalse(@Param("subjectId") Long subjectId);

    @Query("""
        SELECT t FROM Topic t
        WHERE t.isDeleted = false
          AND t.isActive = true
          AND t.subjectId = :subjectId
        """)
    List<Topic> findBySubjectIdAndIsActiveTrueAndIsDeletedFalseForAdaptive(@Param("subjectId") Long subjectId);

    @Query("""
        SELECT t FROM Topic t
        WHERE t.topicId IN :topicIds
          AND t.isDeleted = false
        """)
    List<Topic> findByTopicIdInAndIsDeletedFalse(@Param("topicIds") List<Long> topicIds);

    /**
     * Lấy tất cả topics available cho user trong adaptive learning
     * - Topics active, thuộc subjectId, có >= minQuestions câu hỏi
     * - Prerequisite = null HOẶC prerequisite đã mastered (masteryScore >= masteryThreshold)
     */
    @Query("""
        SELECT t FROM Topic t
        LEFT JOIN UserTopicMastery prereqMastery ON prereqMastery.topicId = t.prerequisiteTopicId
            AND prereqMastery.userId = :userId
            AND prereqMastery.isDeleted = false
        WHERE t.subjectId = :subjectId
          AND t.isDeleted = false
          AND t.isActive = true
          AND (t.prerequisiteTopicId IS NULL 
               OR (prereqMastery.masteryScore IS NOT NULL 
                   AND prereqMastery.masteryScore >= :masteryThreshold))
          AND (SELECT COUNT(q) FROM Question q 
               WHERE q.topicId = t.topicId 
                 AND q.isDeleted = false 
                 AND q.isReviewQuestion = true) >= :minQuestions
        ORDER BY t.createdAt ASC
        """)
    List<Topic> findAvailableTopicsForAdaptiveLearning(
            @Param("subjectId") Long subjectId,
            @Param("userId") Long userId,
            @Param("minQuestions") int minQuestions,
            @Param("masteryThreshold") double masteryThreshold
    );
}
