package com.vn.backend.entities;

import com.vn.backend.enums.ClassMemberRole;
import com.vn.backend.enums.ClassMemberStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private ClassMemberRole memberRole;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 20)
    private ClassMemberStatus memberStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", referencedColumnName = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
}
