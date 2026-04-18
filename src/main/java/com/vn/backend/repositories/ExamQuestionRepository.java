package com.vn.backend.repositories;

import com.vn.backend.dto.request.exam.ExamQuestionsSearchRequestDTO;
import com.vn.backend.dto.response.exam.DifficultyDistributionResponse;
import com.vn.backend.dto.response.exam.TopicDistributionResponse;
import com.vn.backend.entities.ExamQuestion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

  Optional<ExamQuestion> findByExamIdAndQuestionIdAndExam_CreatedBy(
      Long examId,
      Long questionId,
      Long createdBy
  );

  @Query("""
          select eq
          from ExamQuestion eq
          where eq.examId = :#{#filter.examId}
          and eq.exam.isDeleted = false
          and eq.question.isDeleted = false
          and eq.exam.createdBy = :#{#filter.createdBy}
      """)
  Page<ExamQuestion> searchExamQuestion(@Param("filter")ExamQuestionsSearchRequestDTO dto, Pageable pageable);

  @Query("""
          select eq
          from ExamQuestion eq
          where eq.examId = :examId
          and eq.exam.createdBy = :createdBy
          and eq.exam.isDeleted = false
          and eq.question.isDeleted = false
      """)
  List<ExamQuestion> getAllExamQuestion(Long examId, Long createdBy);

  @Query("""
    select distinct eq
    from ExamQuestion eq
    join fetch eq.question q
    left join fetch q.answers a
    where eq.examId = :examId
    and eq.exam.isDeleted = false
    and q.isDeleted = false
  """)
  List<ExamQuestion> getAllExamQuestion(Long examId);

  @Query("""
          SELECT COUNT(eq)
          FROM ExamQuestion eq
          JOIN eq.question q
          WHERE eq.examId = :examId
          AND eq.exam.createdBy = :createdBy
          AND eq.exam.isDeleted =false
          AND q.isDeleted = false
      """)
  long countQuestions(Long examId, Long createdBy);
  @Query("""
        SELECT new com.vn.backend.dto.response.exam.DifficultyDistributionResponse(
            q.difficultyLevel,
            COUNT(q)
        )
        FROM ExamQuestion eq
        JOIN eq.question q
        WHERE eq.examId = :examId
          AND eq.exam.createdBy = :createdBy
          AND eq.exam.isDeleted = false
          AND q.isDeleted = false
        GROUP BY q.difficultyLevel
        ORDER BY q.difficultyLevel
    """)
  List<DifficultyDistributionResponse> countByDifficulty(Long examId, Long createdBy);


  @Query("""
        SELECT new com.vn.backend.dto.response.exam.TopicDistributionResponse(
            t.topicId,
            t.topicName,
            COUNT(q)
        )
        FROM ExamQuestion eq
        JOIN eq.question q
        JOIN q.topic t
        WHERE eq.examId = :examId
          AND eq.exam.createdBy = :createdBy
          AND eq.exam.isDeleted = false
          AND q.isDeleted = false
        GROUP BY t.topicId, t.topicName
        ORDER BY t.topicName
    """)
  List<TopicDistributionResponse> countByTopic( Long examId, Long createdBy);

  @Query("""
      select q.questionId
      from Question q
      join ExamQuestion eq
      on q.questionId = eq.questionId
      where eq.examId = :examId
      and q.isDeleted = false
    """)
  Set<Long> findAllIdsByExamId(Long examId);
}
