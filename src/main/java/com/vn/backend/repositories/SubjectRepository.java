package com.vn.backend.repositories;

import com.vn.backend.dto.request.subject.SubjectSearchRequestDTO;
import com.vn.backend.entities.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubjectCodeAndIsDeletedIsFalse(String subjectCode);

    boolean existsBySubjectIdAndIsDeletedIsFalse(Long subjectId);

    @Query("""
                select s
                from Subject s
                where s.isDeleted = false
                and 
                (
                        (:#{#filter.subjectCode} is null or s.subjectCode like :#{#filter.subjectCode})
                    or  (:#{#filter.subjectName} is null or s.subjectName like :#{#filter.subjectName})
                )
            """)
    Page<Subject> searchSubject(@Param("filter")SubjectSearchRequestDTO dto, Pageable pageable);
}
