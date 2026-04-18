package com.vn.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;


    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Column(name = "is_revoked")
    @Builder.Default
    private Boolean isRevoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
