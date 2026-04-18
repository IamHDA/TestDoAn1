package com.vn.backend.entities;

import com.vn.backend.enums.GradeCalculationMethod;
import com.vn.backend.enums.LateSubmissionPolicy;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "classroom_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomSetting extends BaseEntity{

    @Id // PK & FK
    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "allow_student_post", nullable = false)
    private Boolean allowStudentPost;

    @Column(name = "notify_email", nullable = false)
    private Boolean notifyEmail;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;
}