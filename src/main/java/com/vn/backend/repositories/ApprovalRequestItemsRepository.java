package com.vn.backend.repositories;

import com.vn.backend.entities.ApprovalRequestItems;
import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.EntityType;
import com.vn.backend.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestItemsRepository extends JpaRepository<ApprovalRequestItems, Long> {
    
    List<ApprovalRequestItems> findByRequestIdAndIsDeletedFalse(Long requestId);
    
    /**
     * Lấy các topicIds từ ApprovalRequestItems thuộc các ApprovalRequest PENDING của user
     */
    @Query("""
        SELECT ari.entityId FROM ApprovalRequestItems ari
        JOIN ApprovalRequest ar ON ari.requestId = ar.id
        WHERE ar.isDeleted = false
          AND ari.isDeleted = false
          AND ar.requestType = :requestType
          AND ar.status = :status
          AND ari.entityType = :entityType
        """)
    List<Long> findEntityIdsByPendingRequest(
            @Param("requestType") RequestType requestType,
            @Param("status") ApprovalStatus status,
            @Param("entityType") EntityType entityType);
}
