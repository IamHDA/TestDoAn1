package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "subjects")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subjectId;

    @Column(name = "subject_code", unique = true)
    private String subjectCode;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
