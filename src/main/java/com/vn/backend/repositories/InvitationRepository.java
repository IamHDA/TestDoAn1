package com.vn.backend.repositories;

import com.vn.backend.entities.Invitation;
import com.vn.backend.enums.ClassroomInvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // Kiểm tra xem user đã có lời mời pending cho classroom chưa
    boolean existsByClassroomIdAndUserIdAndInvitationStatus(Long classroomId, Long userId, ClassroomInvitationStatus status);

    // Custom query để lấy thông tin đầy đủ của invitation với classroom và user info
    @Query("SELECT i FROM Invitation i " +
           "LEFT JOIN FETCH i.classroom c " +
           "LEFT JOIN FETCH i.invitedUser u " +
           "LEFT JOIN FETCH i.inviter inv " +
           "WHERE i.invitationId = :invitationId")
    Optional<Invitation> findByIdWithDetails(@Param("invitationId") Long invitationId);

    // Tìm lời mời của user với filter và pagination
    @Query("SELECT i FROM Invitation i " +
           "LEFT JOIN FETCH i.classroom c " +
           "LEFT JOIN FETCH i.invitedUser u " +
           "LEFT JOIN FETCH i.inviter inv " +
           "WHERE i.userId = :userId " +
            "AND (:status IS NULL OR i.invitationStatus = :status)")
    Page<Invitation> findByUserIdWithFilters(@Param("userId") Long userId, 
                                           @Param("status") ClassroomInvitationStatus status, 
                                           Pageable pageable);
}
