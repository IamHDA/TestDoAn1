package com.vn.backend.repositories;

import com.vn.backend.entities.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {

    Optional<FileStorage> findByFileName(String fileName);
}
