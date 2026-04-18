package com.vn.backend.repositories;

import com.vn.backend.dto.request.question.QuestionAvailableSearchRequestDTO;
import com.vn.backend.dto.response.question.QuestionAvailableSearchQueryDTO;
import com.vn.backend.entities.Question;
import com.vn.backend.entities.Topic;
import com.vn.backend.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
        SELECT q FROM Question q
        LEFT JOIN Topic t ON q.topicId = t.topicId
        WHERE q.isDeleted = false
          AND (:type IS NULL OR q.type = :type)
          AND (:difficultyLevel IS NULL OR q.difficultyLevel = :difficultyLevel)
          AND q.createdBy = :userId
          AND (
                (:topicId IS NOT NULL AND q.topicId = :topicId)
                OR (:topicId IS NULL AND (:subjectId IS NULL OR t.subjectId = :subjectId))
              )
            AND (:content IS NULL OR :content = ''
               OR LOWER(q.content) LIKE LOWER(CONCAT('%', :content, '%')))
        """)
    Page<Question> searchQuestions(
            @Param("type") QuestionType type,
            @Param("difficultyLevel") Integer difficultyLevel,
            @Param("topicId") Long topicId,
            @Param("subjectId") Long subjectId,
            @Param("userId") Long userId,
            @Param("content") String content,
            Pageable pageable
    );

    @Query("""
        SELECT q FROM Question q
        LEFT JOIN Topic t ON q.topicId = t.topicId
        WHERE q.isDeleted = false
          AND q.isReviewQuestion = true
          AND (:type IS NULL OR q.type = :type)
          AND (:difficultyLevel IS NULL OR q.difficultyLevel = :difficultyLevel)
          AND (
                (:topicId IS NOT NULL AND q.topicId = :topicId)
                OR (:topicId IS NULL AND (:subjectId IS NULL OR t.subjectId = :subjectId))
              )
            AND (:content IS NULL OR :content = ''
               OR LOWER(q.content) LIKE LOWER(CONCAT('%', :content, '%')))
        """)
    Page<Question> searchQuestionsForAdmin(
            @Param("type") QuestionType type,
            @Param("difficultyLevel") Integer difficultyLevel,
            @Param("topicId") Long topicId,
            @Param("subjectId") Long subjectId,
            @Param("content") String content,
            Pageable pageable
    );


    Optional<Question> findByQuestionIdAndCreatedByAndIsDeletedFalse(Long questionId, Long createdBy);



    @Query("""
        SELECT DISTINCT q
        FROM Question q
        JOIN FETCH q.topic t
        LEFT JOIN FETCH q.answers qa
        WHERE q.isDeleted = false
          AND q.createdBy = :#{#filter.createdBy}
          AND (:#{#filter.subjectId} IS NULL OR t.subjectId = :#{#filter.subjectId})
          AND (:#{#filter.topicId} IS NULL OR q.topicId = :#{#filter.topicId})
          AND (:#{#filter.difficultyLevel} IS NULL OR q.difficultyLevel = :#{#filter.difficultyLevel})
          AND (:#{#filter.type} IS NULL OR q.type = :#{#filter.type})
          AND (:#{#filter.content} IS NULL OR q.content LIKE :#{#filter.content})
    """)
    Page<Question> searchAvailableQuestions(
        @Param("filter") QuestionAvailableSearchRequestDTO filter,
        Pageable pageable
    );


    /**
     * Lấy danh sách topicIds có số câu hỏi >= minQuestions (chỉ đếm câu hỏi ôn tập)
     */
    @Query("""
        SELECT q.topicId 
        FROM Question q 
        WHERE q.isDeleted = false 
          AND q.isReviewQuestion = true
        GROUP BY q.topicId 
        HAVING COUNT(q) >= :minQuestions
        """)
    List<Long> findTopicIdsWithMinimumQuestions(@Param("minQuestions") int minQuestions);


    @Query("""
        SELECT q FROM Question q
        WHERE q.isDeleted = false
          AND q.isReviewQuestion = true
          AND q.topicId = :topicId
          AND q.difficultyLevel = :difficultyLevel
        """)
    List<Question> findByTopicIdAndDifficultyLevelAndIsReviewQuestionTrue(
            @Param("topicId") Long topicId,
            @Param("difficultyLevel") Integer difficultyLevel
    );

    @Query("""
        SELECT q FROM Question q
        WHERE q.isDeleted = false
          AND q.isReviewQuestion = true
          AND q.topicId = :topicId
        """)
    List<Question> findByTopicIdAndIsReviewQuestionTrue(@Param("topicId") Long topicId);

    Optional<Question> findByQuestionIdAndIsDeletedFalse(Long questionId);
}
