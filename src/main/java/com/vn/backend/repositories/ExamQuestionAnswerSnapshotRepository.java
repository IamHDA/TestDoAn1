package com.vn.backend.repositories;

import com.vn.backend.entities.ExamQuestionAnswerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionAnswerSnapshotRepository extends
    JpaRepository<ExamQuestionAnswerSnapshot, Long> {

}
