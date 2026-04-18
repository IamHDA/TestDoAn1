package com.vn.backend.repositories;

import com.vn.backend.dto.request.approval.ApprovalRequestSearchRequestDTO;
import com.vn.backend.entities.ApprovalRequest;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    
    Optional<ApprovalRequest> findByIdAndIsDeletedFalse(Long id);

    @Query("""
        SELECT ar FROM ApprovalRequest ar
        JOIN FETCH ar.requester
        LEFT JOIN FETCH ar.reviewer
        LEFT JOIN FETCH ar.items
        WHERE ar.id = :id
        AND (:userId IS NULL OR ar.requesterId = :userId)
        AND ar.isDeleted = false
        """)
    Optional<ApprovalRequest> findByIdWithDetails(@Param("id") Long id, @Param("userId") Long userId);
    @Query("""
        SELECT ar
        FROM ApprovalRequest ar
        JOIN FETCH ar.requester
        WHERE ar.isDeleted = false
        AND (:#{#filter.userId} IS NULL OR ar.requesterId = :#{#filter.userId})
        AND (:#{#filter.requestType} IS NULL OR ar.requestType = :#{#filter.requestType})
        AND (:#{#filter.status} IS NULL OR ar.status = :#{#filter.status})
        AND (:#{#filter.createdAtFrom} IS NULL OR (ar.createdAt >= :#{#filter.createdAtFrom}))
        AND (:#{#filter.createdAtTo} IS NULL OR (ar.createdAt <= :#{#filter.createdAtTo}))
    """)
    Page<ApprovalRequest> searchApprovalRequest(ApprovalRequestSearchRequestDTO filter, Pageable pageable);
}


