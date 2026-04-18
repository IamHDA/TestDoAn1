package com.vn.backend.entities;

import com.vn.backend.enums.ApprovalStatus;
import com.vn.backend.enums.RequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 100)
    private RequestType requestType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User reviewer;

    @OneToMany(mappedBy = "approvalRequest", fetch = FetchType.LAZY)
    private List<ApprovalRequestItems> items;
}