package com.vn.backend.repositories;

import com.vn.backend.dto.request.assignment.AssigneeSearchRequestDTO;
import com.vn.backend.dto.request.submission.SubmissionSearchRequestDTO;
import com.vn.backend.dto.response.assignment.AssigneeSearchQueryDTO;
import com.vn.backend.dto.response.assignment.AssignmentOverviewQueryDTO;
import com.vn.backend.dto.response.submission.SubmissionExcelQueryDTO;
import com.vn.backend.dto.response.submission.SubmissionSearchQueryDTO;
import com.vn.backend.entities.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findBySubmissionIdAndStudentId(Long submissionId, Long studentId);

    @Query("""
                select new com.vn.backend.dto.response.submission.SubmissionSearchQueryDTO(
                    sm.submissionId,
                    sm.studentId,
                    sm.student.username,
                    sm.student.avatarUrl,
                    sm.student.fullName,
                    sm.submissionStatus,
                    sm.gradingStatus,
                    sm.submittedAt,
                    sm.grade
                )
                from Submission sm
                where sm.assignmentId = :#{#filter.assignmentId}
                and ((:#{#filter.username} is null or sm.student.username like :#{#filter.username})
                    or (:#{#filter.fullName} is null or sm.student.fullName like :#{#filter.fullName}))
                and (:#{#filter.submissionStatus} is null or sm.submissionStatus = :#{#filter.submissionStatus})
                and (:#{#filter.gradingStatus} is null or sm.gradingStatus = :#{#filter.gradingStatus})
            """)
    Page<SubmissionSearchQueryDTO> searchSubmission(@Param("filter") SubmissionSearchRequestDTO dto, Pageable pageable);

    @Query("""
                SELECT 
                    CASE 
                        WHEN (c.teacherId = :userId) THEN TRUE
                        WHEN EXISTS (
                            SELECT 1 
                            FROM ClassMember cm
                            WHERE cm.classroomId = c.classroomId
                              AND cm.userId = :userId
                              AND cm.memberRole = 'ASSISTANT'
                        ) THEN TRUE
                        ELSE FALSE
                    END
                FROM Submission s
                JOIN Assignment a ON s.assignmentId = a.assignmentId
                JOIN Classroom c ON a.classroomId = c.classroomId
                WHERE s.submissionId = :submissionId
            """)
    Boolean hasPermission(@Param("submissionId") Long submissionId,
                          @Param("userId") Long userId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @Query("""
                select new com.vn.backend.dto.response.assignment.AssigneeSearchQueryDTO(
                    cm.user,
                    case when sm.submissionId is not null then true else false end
                )
                from ClassMember cm
                inner join Assignment asm
                on cm.classroomId = asm.classroomId
                left join Submission sm
                on asm.assignmentId = sm.assignmentId
                and cm.userId = sm.studentId
                where asm.assignmentId = :#{#filter.assignmentId}
                and cm.memberStatus = 'ACTIVE'
                and cm.memberRole = 'STUDENT'
                and ((:#{#filter.username} is null or cm.user.username like :#{#filter.username})
                    or (:#{#filter.fullName} is null or cm.user.fullName like :#{#filter.fullName}))
                order by
                    case when sm.submissionId is not null then true else false end asc
            """)
    Page<AssigneeSearchQueryDTO> searchAssignee(@Param("filter") AssigneeSearchRequestDTO dto, Pageable pageable);

    List<Submission> findAllByAssignmentId(Long assignmentId);

    @Query("""
                SELECT new com.vn.backend.dto.response.submission.SubmissionExcelQueryDTO(
                    s.submissionId,
                    s.student.username,
                    s.student.fullName,
                    s.student.code,
                    s.submissionStatus,
                    s.gradingStatus,
                    s.updatedAt,
                    s.grade
                )
                FROM Submission s
                WHERE s.assignmentId = :assignmentId
            """)
    List<SubmissionExcelQueryDTO> findAllForExcel(Long assignmentId);


    @Query("""
                SELECT new com.vn.backend.dto.response.assignment.AssignmentOverviewQueryDTO(
                    COUNT(sm), 
                    SUM(CASE WHEN sm.submissionStatus = com.vn.backend.enums.SubmissionStatus.SUBMITTED THEN 1 ELSE 0 END),
                    SUM(CASE WHEN sm.submissionStatus = com.vn.backend.enums.SubmissionStatus.NOT_SUBMITTED THEN 1 ELSE 0 END),
                    SUM(CASE WHEN sm.submissionStatus = com.vn.backend.enums.SubmissionStatus.LATE_SUBMITTED THEN 1 ELSE 0 END),
                    SUM(CASE WHEN sm.gradingStatus = com.vn.backend.enums.GradingStatus.GRADED THEN 1 ELSE 0 END),
                    AVG(sm.grade),
                    MAX(sm.grade),
                    MIN(sm.grade)
                )
                FROM Submission sm
                WHERE sm.assignmentId = :assignmentId
            """)
    AssignmentOverviewQueryDTO getAssignmentOverview(Long assignmentId);

    @Query("""
                SELECT sm.grade
                FROM Submission sm
                WHERE sm.assignmentId = :assignmentId
                  AND sm.grade IS NOT NULL
                ORDER BY sm.grade ASC
            """)
    List<Double> findAllGradesByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * Tính điểm trung bình trực tiếp từ DB
     */
    @Query("""
        SELECT AVG(sm.grade)
        FROM Submission sm
        WHERE sm.assignmentId = :assignmentId
        AND sm.grade IS NOT NULL
        """)
    Double getAverageGradeByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * Đếm tổng số sinh viên được assign vào assignment
     */
    @Query("""
        SELECT COUNT(DISTINCT sm.studentId)
        FROM Submission sm
        WHERE sm.assignmentId = :assignmentId
        """)
    Long countTotalStudentsByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * Đếm số sinh viên đã nộp bài (SUBMITTED hoặc LATE_SUBMITTED) trong một assignment
     */
    @Query("""
        SELECT COUNT(DISTINCT sm.studentId)
        FROM Submission sm
        WHERE sm.assignmentId = :assignmentId
        AND sm.submissionStatus IN (com.vn.backend.enums.SubmissionStatus.SUBMITTED, com.vn.backend.enums.SubmissionStatus.LATE_SUBMITTED)
        """)
    Long countSubmittedStudentsByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * Tính pass rate (>= 5.0) trực tiếp từ DB
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(sm) > 0 THEN 
                    CAST(SUM(CASE WHEN sm.grade >= 5.0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(sm) * 100
                ELSE 0.0
            END
        FROM Submission sm
        WHERE sm.assignmentId = :assignmentId
        AND sm.grade IS NOT NULL
        """)
    Double getPassRateByAssignmentId(@Param("assignmentId") Long assignmentId);

    /**
     * Tính excellent rate (>= 8.0) trực tiếp từ DB
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(sm) > 0 THEN 
                    CAST(SUM(CASE WHEN sm.grade >= 8.0 THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(sm) * 100
                ELSE 0.0
            END
        FROM Submission sm
        WHERE sm.assignmentId = :assignmentId
        AND sm.grade IS NOT NULL
        """)
    Double getExcellentRateByAssignmentId(@Param("assignmentId") Long assignmentId);

}
