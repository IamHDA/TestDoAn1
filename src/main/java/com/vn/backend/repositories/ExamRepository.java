package com.vn.backend.repositories;

import com.vn.backend.dto.request.exam.ExamSearchRequestDTO;
import com.vn.backend.entities.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByExamIdAndIsDeletedIsFalse(Long examId);

    Optional<Exam> findByExamIdAndCreatedByAndIsDeletedIsFalse(Long examId, Long createdBy);

    @Query("""
        select e
        from Exam e
        where e.isDeleted = false
        and e.subject.isDeleted = false
        and e.createdBy = :#{#filter.createdBy}
        and (:#{#filter.subjectId} is null or e.subjectId = :#{#filter.subjectId})
        and (:#{#filter.title} is null or e.title like :#{#filter.title}
            or :#{#filter.description} is null or e.description like :#{#filter.description}
        )
        """)
    Page<Exam> searchExam(@Param("filter")ExamSearchRequestDTO dto, Pageable pageable);
}
