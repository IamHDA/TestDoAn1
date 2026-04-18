package com.vn.backend.entities;

import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassroomInvitationStatus;
import com.vn.backend.enums.ClassroomInvitationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    // FK -> Classroom
    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    // Người được mời hoặc người gửi request
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Loại lời mời (INVITE: giáo viên mời, REQUEST: sinh viên xin tham gia)
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_type", nullable = false, length = 20)
    private ClassroomInvitationType invitationType;

    // Vai trò trong lớp (TEACHER, STUDENT, ASSISTANT)
    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private ClassMemberRole memberRole;

    // Người tạo lời mời (giáo viên hoặc hệ thống), có thể NULL nếu là request từ sinh viên
    @Column(name = "invited_by")
    private Long invitedBy;

    // Dùng khi share link/email (không cần nếu chỉ dùng classCode)
    @Column(name = "classroom_code", unique = true, length = 100)
    private String classroomCode;

    // Trạng thái lời mời
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 20)
    private ClassroomInvitationStatus invitationStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Quan hệ tới Classroom
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;

    // Quan hệ tới User được mời / gửi request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User invitedUser;

    // Quan hệ tới User mời (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User inviter;
}
