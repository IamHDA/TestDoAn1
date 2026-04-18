package com.vn.backend.entities;

import com.vn.backend.enums.AnnouncementType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    private Long announcementId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;          // tiêu đề thông báo

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;        // nội dung thông báo (hỗ trợ HTML/Markdown)

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "announcement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AnnouncementType type;

    @Column(name = "allow_comments", nullable = false)
    @Builder.Default
    private Boolean allowComments = true;  // cho phép bình luận

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", referencedColumnName = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;

    @Column(name = "object_id", nullable = true)
    private Long objectId;
}
