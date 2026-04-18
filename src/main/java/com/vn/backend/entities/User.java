package com.vn.backend.entities;

import com.vn.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String code;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatarUrl")
    private String avatarUrl;    //  ảnh

    @Column(name = "is_active",nullable = false)
    private Boolean isActive;

    @Column(name = "is_deleted",nullable = false)
    private Boolean isDeleted;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

}
