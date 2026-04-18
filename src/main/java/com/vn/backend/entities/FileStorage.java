package com.vn.backend.entities;

import com.vn.backend.enums.ProviderStorage;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_storage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "url")
    private String url;

    @Column(name = "storage_path")
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_storage")
    private ProviderStorage providerStorage;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}

