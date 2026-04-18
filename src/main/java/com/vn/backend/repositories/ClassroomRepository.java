package com.vn.backend.repositories;

import com.vn.backend.dto.request.classroom.ClassroomSearchRequestDTO;
import com.vn.backend.dto.response.classroom.ClassroomSearchQueryDTO;
import com.vn.backend.entities.Classroom;
import com.vn.backend.enums.ClassroomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByClassCode(String classCode);

    Optional<Classroom> findByClassroomIdAndClassroomStatus(Long id, ClassroomStatus status);

    Optional<Classroom> findByClassCode(String classCode);

    @Query("""
    SELECT new com.vn.backend.dto.response.classroom.ClassroomSearchQueryDTO(
        c.classroomId,
        c.className,
        c.subject,
        c.coverImageUrl,
        c.classroomStatus,
        c.teacher.id,
        c.teacher.username,
        c.teacher.fullName,
        c.teacher.avatarUrl,
        COUNT(DISTINCT cmAll.userId),
        COUNT(DISTINCT a.assignmentId),
        c.createdAt
    )
    FROM Classroom c
    LEFT JOIN ClassMember cm
        ON cm.classroomId = c.classroomId 
        AND cm.userId = :#{#filter.userId}
        AND cm.memberStatus = :#{#filter.classMemberStatus}
    LEFT JOIN ClassMember cmAll
        ON cmAll.classroomId = c.classroomId And cmAll.memberStatus != 'INACTIVE'
    LEFT JOIN Assignment a
        ON a.classroomId = c.classroomId
             AND a.isDeleted = false
    WHERE 
        (c.teacherId = :#{#filter.userId} OR cm.userId = :#{#filter.userId})
        AND (:#{#filter.teacherName} is null or c.teacher.fullName like :#{#filter.teacherName})
        AND (:#{#filter.className} is null or c.className like :#{#filter.className})
        AND (:#{#filter.subjectId} is null or c.subjectId = :#{#filter.subjectId})
        AND (:#{#filter.classroomStatus} is null or c.classroomStatus = :#{#filter.classroomStatus})
        AND c.isActive = true
    GROUP BY
            c.classroomId,
            c.className,
            c.subject,
            c.coverImageUrl,
            c.classroomStatus,
            c.teacher.id,
            c.teacher.username,
            c.teacher.fullName,
            c.teacher.avatarUrl
    """)
    Page<ClassroomSearchQueryDTO> searchClassroom(
            @Param("filter") ClassroomSearchRequestDTO requestDTO, Pageable pageable
    );

    Optional<Classroom> findByClassroomIdAndTeacherIdAndIsActiveTrue(Long classroomId, Long teacherId);

    Optional<Classroom> findByClassroomIdAndClassroomStatusAndIsActiveTrue(Long classroomId, ClassroomStatus status);

    // Kiểm tra xem user có phải là teacher của classroom không
    boolean existsByClassroomIdAndTeacherId(Long classroomId, Long userId);

}
