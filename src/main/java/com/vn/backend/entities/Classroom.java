package com.vn.backend.entities;

import com.vn.backend.enums.ClassCodeStatus;
import com.vn.backend.enums.ClassroomStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "classroom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "class_code", nullable = false, unique = true, length = 50)
    private String classCode;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ClassroomStatus classroomStatus;

    @Column(name = "class_code_status")
    @Enumerated(EnumType.STRING)
    private ClassCodeStatus classCodeStatus;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "course_outline_url", nullable = false)
    private String courseOutlineUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @OneToMany(mappedBy = "classroom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassSchedule> schedules = new ArrayList<>();
}
