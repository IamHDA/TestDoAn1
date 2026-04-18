package com.vn.backend.repositories;

import com.vn.backend.entities.User;
import com.vn.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndIsActive(String username, boolean active);
    Optional<User> findByCodeAndIsActive(String code, boolean active);
    @Query("SELECT u FROM User u")
    Page<User> findUsers(Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(u.username) LIKE LOWER(:searchTerm) OR " +
           "LOWER(u.fullName) LIKE LOWER(:searchTerm) OR " +
           "LOWER(u.email) LIKE LOWER(:searchTerm)) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.isActive = :status)")
    Page<User> findByFilters(@Param("searchTerm") String searchTerm, 
                            @Param("role") com.vn.backend.enums.Role role,
                            @Param("status") Boolean status,
                            Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "u.isActive = true AND " +
           "u.isDeleted = false AND " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           // Filter by role (chỉ STUDENT và TEACHER)
           "(:role IS NULL OR u.role = :role) AND " +
           "(u.role = 'STUDENT' OR u.role = 'TEACHER') AND " +
            "u.id != :userId AND "  +
           // Bỏ qua teacher của classroom hiện tại
           "u.id != (SELECT c.teacherId FROM Classroom c WHERE c.classroomId = :classroomId) AND " +
           // User chưa thuộc lớp học hiện tại
           "u.id NOT IN (SELECT cm.userId FROM ClassMember cm WHERE cm.classroomId = :classroomId AND cm.memberStatus = 'ACTIVE') AND " +
           // User chưa có lời mời nào HOẶC có lời mời DECLINED cho lớp hiện tại
           "(u.id NOT IN (SELECT i.userId FROM Invitation i WHERE i.classroomId = :classroomId) OR " +
           "u.id IN (SELECT i2.userId FROM Invitation i2 WHERE i2.classroomId = :classroomId AND i2.invitationStatus = 'DECLINED'))")
    Page<User> findUsersForInvite(@Param("searchTerm") String searchTerm,
                                  @Param("role") Role role,
                                  @Param("classroomId") Long classroomId,
                                  @Param("userId") Long userId,
                                  Pageable pageable);

    @Query("SELECT u.code FROM User u WHERE u.isActive = true AND u.isDeleted = false AND LOWER(u.code) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<String> findCodesByPrefix(@Param("prefix") String prefix);

    List<User> findAllByIdInAndIsDeletedFalse(List<Long> ids);
}
