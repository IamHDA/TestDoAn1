package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "assignments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column
    private Long maxScore; // Điểm tối đa

    @Column
    private LocalDateTime dueDate;   // Hạn nộp bài

    @Column
    private boolean submissionClosed; // Đóng/mở tính năng nộp bài sau hạn

    @Column(name = "title", nullable = false, length = 255)
    private String title;          // tiêu đề thông báo

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;        // nội dung thông báo (hỗ trợ HTML/Markdown)

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;


    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", referencedColumnName = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;

//    @Column(name = "announcement_id", insertable = false, updatable = false)
//    private Long announcementId;
//
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "announcement_id", referencedColumnName = "announcement_id")
//    private Announcement announcement;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
