package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "class_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "day_of_week", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 50)
    private String room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", insertable = false, updatable = false)
    private Classroom classroom;
}