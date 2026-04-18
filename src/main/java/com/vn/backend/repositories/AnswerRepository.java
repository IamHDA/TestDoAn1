package com.vn.backend.repositories;

import com.vn.backend.entities.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    // Find answers by questionId
    List<Answer> findByQuestionIdOrderByDisplayOrder(Long questionId);

    Optional<Answer> findByAnswerIdAndIsDeletedFalse(Long answerId);

    long countByQuestionIdAndIsCorrectTrue(Long questionId);



    long countByQuestionIdAndIsCorrectTrueAndAnswerIdNotAndIsDeletedFalse(Long questionId, Long answerId);

    Optional<Answer> findByAnswerIdAndIsCorrectTrueAndIsDeletedFalse(Long answerId);

    /**
     * Lấy correct answers theo questionId
     */
    @Query("""
        SELECT a FROM Answer a
        WHERE a.isDeleted = false
          AND a.questionId = :questionId
          AND a.isCorrect = true
        """)
    List<Answer> findCorrectAnswersByQuestionId(@Param("questionId") Long questionId);
}
