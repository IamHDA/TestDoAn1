package com.vn.backend.entities;
import com.vn.backend.enums.AttachmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attachmentId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "description", length = 500)
    private String description;

    // Link tới announcement, assignment (polymorphic association)
    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "attachment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AttachmentType attachmentType;

    // Link tới user upload
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User uploadedByUser;
}
