package com.vn.backend.repositories;

import com.vn.backend.entities.Assignment;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import com.vn.backend.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByAssignmentId(Long id);
    // Find assignment by ID that is not deleted
    @Query("SELECT a FROM Assignment a WHERE a.assignmentId = :assignmentId AND a.isDeleted = false")
    Optional<Assignment> findByAssignmentIdAndNotDeleted(@Param("assignmentId") Long assignmentId);

    // Find assignments by classroom ID with pagination
    @Query("SELECT a FROM Assignment a WHERE a.classroomId = :classroomId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    Page<Assignment> findByClassroomIdAndNotDeletedWithPagination(@Param("classroomId") Long classroomId, Pageable pageable);

    @Query("""
                SELECT a
                FROM Assignment a
                JOIN ClassMember cm ON a.classroomId = cm.classroomId
                WHERE a.assignmentId = :assignmentId
                  AND cm.userId = :userId
                  AND cm.memberRole = :memberRole
                  AND cm.memberStatus = :memberStatus
            """)
    Optional<Assignment> findAssignmentIfUserCanSubmit(Long userId, Long assignmentId, ClassMemberRole memberRole, ClassMemberStatus memberStatus);

    @Query("""
        SELECT 
            CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM Assignment a
        JOIN Classroom c ON a.classroomId = c.classroomId
        LEFT JOIN ClassMember m 
            ON m.classroomId = c.classroomId 
            AND m.userId = :userId 
            AND m.memberStatus = com.vn.backend.enums.ClassMemberStatus.ACTIVE
        WHERE a.assignmentId = :assignmentId
        AND (
            c.teacherId = :userId
            OR m.memberRole = com.vn.backend.enums.ClassMemberRole.ASSISTANT
        )
    """)
    Boolean canUserViewSubmissions(Long assignmentId, Long userId);

    /**
     * Lấy tất cả assignments của một classroom đã có ít nhất 1 submission được chấm điểm và chưa bị xóa
     */
    @Query("""
        SELECT DISTINCT a
        FROM Assignment a
        INNER JOIN Submission s ON s.assignmentId = a.assignmentId
        WHERE a.classroomId = :classroomId
        AND a.isDeleted = false
        AND s.grade IS NOT NULL
        ORDER BY a.createdAt ASC
        """)
    List<Assignment> findAssignmentsWithGradedSubmissionsByClassroomId(
        @Param("classroomId") Long classroomId
    );

    /**
     * Lấy tất cả assignments của một classroom chưa bị xóa
     */
    @Query("""
        SELECT a
        FROM Assignment a
        WHERE a.classroomId = :classroomId
        AND a.isDeleted = false
        ORDER BY a.createdAt ASC
        """)
    List<Assignment> findAllByClassroomIdAndIsDeletedFalse(@Param("classroomId") Long classroomId);
}
