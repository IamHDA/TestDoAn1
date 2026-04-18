package com.vn.backend.repositories;

import com.vn.backend.entities.Announcement;
import com.vn.backend.enums.AnnouncementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a " +
           "WHERE a.classroomId = :classroomId " +
           "AND a.isDeleted = false " +
           "AND (:type IS NULL OR a.type = :type) "+
           "ORDER BY a.updatedAt DESC")
    Page<Announcement> findByClassroomIdWithFilters(
            @Param("classroomId") Long classroomId,
            @Param("type") AnnouncementType type,
            Pageable pageable);

    @Query("SELECT a FROM Announcement a " +
           "WHERE a.announcementId = :announcementId " +
           "AND a.isDeleted = false")
    Optional<Announcement> findByIdAndNotDeleted(@Param("announcementId") Long announcementId);


    Announcement findByObjectIdAndType(Long assignmentId, AnnouncementType announcementType);
}