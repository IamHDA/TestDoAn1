package com.vn.backend.entities;

import com.vn.backend.enums.EntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_request_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestItems extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 100)
    private EntityType entityType;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ApprovalRequest approvalRequest;
}