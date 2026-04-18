package com.vn.backend.repositories;

import com.vn.backend.dto.response.studentsessionexam.StudentExamResultQueryDTO;
import com.vn.backend.entities.StudentSessionExam;
import com.vn.backend.entities.User;
import com.vn.backend.enums.ExamSubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSessionExamRepository extends JpaRepository<StudentSessionExam, Long> {

    Optional<StudentSessionExam> findBySessionExamIdAndStudentIdAndIsDeletedFalse(Long sessionExamId, Long studentId);

    boolean existsBySessionExamIdAndStudentIdAndIsDeletedFalse(Long sessionExamId, Long studentId);



    @Query("""
            SELECT 
                sse.studentSessionExamId,
                :sessionExamId,
                u.id,
                u.fullName,
                u.username,
                u.code,
                u.email,
                u.avatarUrl,
                sse.score,
                COALESCE(sse.submissionStatus, com.vn.backend.enums.ExamSubmissionStatus.NOT_STARTED),
                sse.examStartTime,
                sse.submissionTime,
                sse.createdAt
            FROM ClassMember cm
            INNER JOIN cm.user u
            INNER JOIN StudentSessionExam sse ON sse.studentId = u.id 
                AND sse.sessionExamId = :sessionExamId 
            WHERE cm.classroomId = :classroomId 
                AND cm.memberRole = com.vn.backend.enums.ClassMemberRole.STUDENT
                AND cm.memberStatus = com.vn.backend.enums.ClassMemberStatus.ACTIVE
            ORDER BY u.fullName ASC
            """)
    Page<Object[]> getExamResults(
            @Param("sessionExamId") Long sessionExamId,
            @Param("classroomId") Long classroomId,
            Pageable pageable
    );

    /**
     * Tìm tất cả StudentSessionExam của bài thi FLEXIBLE đang làm bài (chưa submit):
     * - examStartTime != null (đã bắt đầu làm bài)
     * - score == null (chưa có điểm)
     * - submissionStatus != NOT_SUBMITTED (chưa nộp bài)
     * - isDeleted = false
     * - SessionExam có examMode = FLEXIBLE
     */
    @Query("""
            SELECT sse
            FROM StudentSessionExam sse
            INNER JOIN SessionExam se ON se.sessionExamId = sse.sessionExamId
            WHERE sse.examStartTime IS NOT NULL
            AND sse.score IS NULL
            AND sse.submissionStatus = :submittedStatus
            AND se.examMode = 'FLEXIBLE'
            AND se.isDeleted = false
            """)
    List<StudentSessionExam> findActiveFlexExams(
            @Param("submittedStatus") ExamSubmissionStatus submittedStatus
    );

    List<StudentSessionExam> findAllBySessionExamId(Long sessionExamId);

    /**
     * Lấy tất cả scores của sinh viên đã nộp bài (có điểm) trong một ca thi
     */
    @Query("""
            SELECT sse.score
            FROM StudentSessionExam sse
            WHERE sse.sessionExamId = :sessionExamId
            AND sse.isDeleted = false
            AND sse.score IS NOT NULL
            ORDER BY sse.score ASC
            """)
    List<Double> findAllScoresBySessionExamId(@Param("sessionExamId") Long sessionExamId);

    /**
     * Đếm tổng số sinh viên tham gia (đã join) trong một ca thi
     */
    @Query("""
            SELECT COUNT(DISTINCT sse.studentId)
            FROM StudentSessionExam sse
            WHERE sse.sessionExamId = :sessionExamId
            """)
    Long countTotalStudentsBySessionExamId(@Param("sessionExamId") Long sessionExamId);

    /**
     * Đếm số sinh viên đã nộp bài (SUBMITTED) trong một ca thi
     */
    @Query("""
            SELECT COUNT(DISTINCT sse.studentId)
            FROM StudentSessionExam sse
            WHERE sse.sessionExamId = :sessionExamId
            AND sse.submissionStatus = :submittedStatus
            """)
    Long countSubmittedStudentsBySessionExamId(
            @Param("sessionExamId") Long sessionExamId,
            @Param("submittedStatus") ExamSubmissionStatus submittedStatus
    );



    /**
     * Tìm tất cả StudentSessionExam với SessionExam đã qua endDate nhưng sinh viên chưa bắt đầu làm bài:
     * - SessionExam có examMode = FLEXIBLE
     * - SessionExam đã qua endDate (endDate < now)
     * - StudentSessionExam chưa bắt đầu (submissionStatus = NOT_STARTED, examStartTime IS NULL)
     */
    @Query("""
            SELECT sse
            FROM StudentSessionExam sse
            INNER JOIN FETCH sse.sessionExam se
            WHERE se.examMode = 'FLEXIBLE'
            AND se.endDate < :now
            AND se.isDeleted = false
            AND sse.submissionStatus = :submissionStatus
            AND sse.examStartTime IS NULL
            AND sse.score IS NULL
            """)
    List<StudentSessionExam> findNotStartedExpiredFlexExamsWithSessionExam(
            @Param("now") LocalDateTime now,
            @Param("submissionStatus") ExamSubmissionStatus submissionStatus
    );

    @Query(value = """
                select 
                    sse.student_session_exam_id AS studentSessionExamId,
                    sse.score AS score,
                    sse.submission_result AS submissionResult,
                    se.exam_mode as examMode
                from student_session_exams sse
                join session_exams se on sse.session_exam_id = se.session_exam_id and se.is_deleted = false
                where sse.student_session_exam_id = :studentSessionExamId and sse.is_deleted = false
                and sse.submission_status = 'SUBMITTED'
                and se.created_by =:userId
            """, nativeQuery = true)
    StudentExamResultQueryDTO getStudentExamResult(@Param("studentSessionExamId") Long studentSessionExamId,
                                                   @Param("userId") Long userId);
}

