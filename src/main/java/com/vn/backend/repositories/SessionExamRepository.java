package com.vn.backend.repositories;

import com.vn.backend.dto.request.sessionexam.SessionExamSearchStudentRequestDTO;
import com.vn.backend.dto.request.sessionexam.SessionExamSearchTeacherRequestDTO;
import com.vn.backend.dto.response.sessionexam.SessionExamStudentQueryDTO;
import com.vn.backend.dto.response.sessionexam.SessionExamTeacherQueryDTO;
import com.vn.backend.entities.SessionExam;
import com.vn.backend.enums.ExamMode;
import com.vn.backend.enums.ExamSubmissionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionExamRepository extends JpaRepository<SessionExam, Long> {

  Optional<SessionExam> findBySessionExamIdAndCreatedByAndIsDeletedFalse(Long id, Long createdBy);

  Optional<SessionExam> findBySessionExamIdAndIsDeletedFalse(Long sessionExamId);

  @Query("""
      select new com.vn.backend.dto.response.sessionexam.SessionExamTeacherQueryDTO(
          se.sessionExamId,
          se.classId,
          se.classroom.className,
          se.title,
          se.duration,
          se.examMode,
          se.startDate,
          se.endDate
      )
      from SessionExam se
      where se.isDeleted = false
      and se.createdBy = :#{#filter.createdBy}
      and (:#{#filter.classId} is null or se.classId = :#{#filter.classId})
      and (
          :#{#filter.title} is null or se.title like :#{#filter.title}
          or :#{#filter.description} is null or se.description like :#{#filter.description}
          )
      and (:#{#filter.examMode} is null or se.examMode = :#{#filter.examMode})
      and (          
              :#{#filter.startDate} is null or se.endDate >= :#{#filter.startDate}
              or :#{#filter.endDate} is null or se.startDate <= :#{#filter.endDate}
      )
      """)
  Page<SessionExamTeacherQueryDTO> searchByTeacher(
      @Param("filter") SessionExamSearchTeacherRequestDTO dto, Pageable pageable);

  @Query("""
      select new com.vn.backend.dto.response.sessionexam.SessionExamStudentQueryDTO(
          se.sessionExamId,
          se.classId,
          se.classroom.className,
          se.title,
          se.duration,
          se.examMode,
          se.startDate,
          se.endDate,
          sse.submissionStatus
      )
      from SessionExam se
      join StudentSessionExam sse
      on se.sessionExamId = sse.sessionExamId and sse.studentId = :#{#filter.studentId}
      where se.isDeleted = false
      and (:#{#filter.classId} is null or se.classId = :#{#filter.classId})
      and (
          :#{#filter.title} is null or se.title like :#{#filter.title}
          or :#{#filter.description} is null or se.description like :#{#filter.description}
          )
      and (:#{#filter.examMode} is null or se.examMode = :#{#filter.examMode})
      and (        
              :#{#filter.startDate} is null or se.endDate >= :#{#filter.startDate}
              or :#{#filter.endDate} is null or se.startDate <= :#{#filter.endDate}
      )
      """)
  Page<SessionExamStudentQueryDTO> searchByStudent(
      @Param("filter") SessionExamSearchStudentRequestDTO dto, Pageable pageable);

  /**
   * Lấy tất cả session exams của một classroom chưa bị xóa
   */
  @Query("""
      SELECT se
      FROM SessionExam se
      WHERE se.classId = :classroomId
      AND se.isDeleted = false
      ORDER BY se.startDate ASC
      """)
  List<SessionExam> findAllByClassroomIdAndIsDeletedFalse(@Param("classroomId") Long classroomId);

  /**
   * Lấy tất cả session exams FLEXIBLE đã qua endDate và chưa bị xóa
   */
  @Query("""
      SELECT se
      FROM SessionExam se
      WHERE se.examMode = :examMode
      AND se.isDeleted = false
      AND se.endDate < :now
      ORDER BY se.endDate ASC
      """)
  List<SessionExam> findExpiredFlexExams(
      @Param("examMode") ExamMode examMode,
      @Param("now") LocalDateTime now
  );

  /**
   * Lấy tất cả session exams của một classroom đã có ít nhất 1 sinh viên nộp bài và chưa bị xóa
   */
  @Query("""
      SELECT DISTINCT se
      FROM SessionExam se
      INNER JOIN StudentSessionExam sse ON sse.sessionExamId = se.sessionExamId
      WHERE se.classId = :classroomId
      AND se.isDeleted = false
      AND sse.isDeleted = false
      AND sse.submissionStatus = :submittedStatus
      ORDER BY se.startDate ASC
      """)
  List<SessionExam> findSessionExamsWithSubmissionsByClassroomId(
      @Param("classroomId") Long classroomId,
      @Param("submittedStatus") ExamSubmissionStatus submittedStatus
  );

  @Query("""
          select se from SessionExam se
          where se.isDeleted = false
          and se.examMode = 'LIVE'
          and se.startDate > :now
          and se.startDate <= :initWindow
          and se.startScheduler = false
          order by se.startDate asc
          """)
  List<SessionExam> findLiveSessionExamsToInit(
          @Param("now") LocalDateTime now,
          @Param("initWindow") LocalDateTime initWindow
  );

  @Query("""
          select se from SessionExam se
          where se.isDeleted = false
          and se.examMode = 'LIVE'
          and se.endDate <= :now
          and se.endDate > :windowStart
          and se.endScheduler = false
          and se.startScheduler = true
          order by se.endDate asc
      """)
  List<SessionExam> findEndingLiveSessionExams(
      @Param("now") LocalDateTime now,
      @Param("windowStart") LocalDateTime windowStart
  );
}


