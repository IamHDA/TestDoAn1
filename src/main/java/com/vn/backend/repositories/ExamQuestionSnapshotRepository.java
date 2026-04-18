package com.vn.backend.repositories;

import com.vn.backend.entities.ExamQuestionSnapshot;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionSnapshotRepository extends JpaRepository<ExamQuestionSnapshot, Long> {

  @Query("""
    select distinct eqs
    from ExamQuestionSnapshot eqs
    left join fetch eqs.examQuestionAnswers
    where eqs.sessionExamId = :sessionExamId
    order by eqs.orderIndex
    """)
  List<ExamQuestionSnapshot> findAllBySessionExamId(Long sessionExamId, Sort sort);

  @Query("""
    select distinct eqs
    from ExamQuestionSnapshot eqs
    left join fetch eqs.examQuestionAnswers
    where eqs.sessionExamId = :sessionExamId
    order by eqs.orderIndex
    """)
  List<ExamQuestionSnapshot> findAllBySessionExamId(Long sessionExamId);

  @Query("""
    select count(distinct eqs.id)
    from ExamQuestionSnapshot eqs
    where eqs.sessionExamId = :sessionExamId
    """)
  Long countBySessionExamId(Long sessionExamId);
}
