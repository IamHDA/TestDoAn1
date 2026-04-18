package com.vn.backend.repositories;

import com.vn.backend.dto.request.classroom.ClassMemberSearchRequestDTO;
import com.vn.backend.dto.response.classroom.ClassMemberSearchQueryDTO;
import com.vn.backend.entities.ClassMember;
import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    Optional<ClassMember> findByClassroomIdAndUserId(Long classroomId, Long userId);

    Optional<ClassMember> findByClassroomIdAndUserIdAndMemberRoleAndMemberStatus(Long classroomId, Long userId, ClassMemberRole role, ClassMemberStatus status);


    // Kiểm tra xem user đã là member trước đây trong classroom chưa
    boolean existsByClassroomIdAndUserIdAndMemberStatus(Long classroomId, Long userId, ClassMemberStatus memberStatus);

    boolean existsByClassroomIdAndUserIdAndMemberStatusAndMemberRole(Long classroomId, Long userId, ClassMemberStatus memberStatus, ClassMemberRole memberRole);

    // Tìm member trước đây trong classroom chưa (trạng thái inactive)
    ClassMember findByClassroomIdAndUserIdAndMemberStatus(Long classroomId, Long userId, ClassMemberStatus memberStatus);

    // Tìm tất cả member theo classroom, role và status
    List<ClassMember> findByClassroomIdAndMemberRoleAndMemberStatus(Long classroomId, ClassMemberRole memberRole, ClassMemberStatus memberStatus);

    List<ClassMember> findByClassroomIdAndMemberRoleAndMemberStatusAndUserIdNot(Long classroomId, ClassMemberRole memberRole, ClassMemberStatus memberStatus, Long userId);

    @Query("""
                 select new com.vn.backend.dto.response.classroom.ClassMemberSearchQueryDTO(
                      cm.memberId,
                      cm.user.id,
                      cm.user.fullName,
                      cm.user.username,
                      cm.user.code,
                      cm.user.email,
                      cm.user.phone,
                      cm.user.avatarUrl,
                      cm.memberRole,
                      cm.memberStatus,
                      cm.joinedAt
                      )
                 from ClassMember cm
                 where cm.classroomId = :#{#filter.classroomId}
                 and (:#{#filter.keyword} is null or cm.user.fullName like :#{#filter.keyword}
                     or cm.user.username like :#{#filter.keyword}
                     or cm.user.code like :#{#filter.keyword}
                     or cm.user.email like :#{#filter.keyword}
                     or cm.user.phone like :#{#filter.keyword})
                 and (:#{#filter.classMemberRole} is null or cm.memberRole = :#{#filter.classMemberRole})
                 and (:#{#filter.classMemberStatus} is null or cm.memberStatus = :#{#filter.classMemberStatus})
            """)
    Page<ClassMemberSearchQueryDTO> searchClassMember(@Param("filter") ClassMemberSearchRequestDTO requestDTO, Pageable pageable);

    @Query("""
            select cm.userId
            from ClassMember cm
            where cm.classroomId = :classroomId
            and (:memberRole is null or cm.memberRole = :memberRole)
            and cm.memberStatus = 'ACTIVE'
            """)
    Set<Long> getClassMemberIdsActive(Long classroomId, ClassMemberRole memberRole);

    @Query(value = "SELECT u.id, u.full_name, u.username, u.code, u.email, u.avatar_url, "+
            "CASE WHEN sse.student_session_exam_id IS NULL THEN 0 ELSE 1 END AS is_joined "+
            "FROM class_members cm " +
            "INNER JOIN users u ON cm.user_id = u.id " +
            "LEFT JOIN student_session_exams sse ON sse.student_id = u.id AND sse.session_exam_id = :sessionExamId AND sse.is_deleted = FALSE " +
            "WHERE cm.classroom_id = :classroomId AND cm.member_role = 'STUDENT' AND cm.member_status = 'ACTIVE' " +
            "AND (:keyword IS NULL OR u.full_name LIKE :keyword OR u.username LIKE :keyword OR u.code LIKE :keyword OR u.email LIKE :keyword) " +
            "ORDER BY u.full_name ASC",
            countQuery = "SELECT COUNT(*) FROM class_members cm " +
                "INNER JOIN users u ON cm.user_id = u.id " +
                "WHERE cm.classroom_id = :classroomId AND cm.member_role = 'STUDENT' AND cm.member_status = 'ACTIVE' " +
            "AND (:keyword IS NULL OR u.full_name LIKE :keyword OR u.username LIKE :keyword OR u.code LIKE :keyword OR u.email LIKE :keyword) ",
            nativeQuery = true)
    Page<Object[]> searchStudentsWithJoinStatus(@Param("classroomId") Long classroomId, @Param("sessionExamId") Long sessionExamId, @Param("keyword") String keyword, Pageable pageable);
}