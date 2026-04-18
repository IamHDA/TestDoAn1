package com.vn.backend.repositories;

import com.vn.backend.entities.Attachment;
import com.vn.backend.enums.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByAttachmentIdAndUploadedByAndIsDeletedEquals(Long attachmentId, Long updatedBy, boolean isDeleted);

    // Polymorphic queries
    @Query("SELECT a FROM Attachment a WHERE a.objectId = :objectId AND a.attachmentType = :attachmentType AND a.isDeleted = false")
    List<Attachment> findByObjectIdAndAttachmentTypeAndNotDeleted(@Param("objectId") Long objectId, @Param("attachmentType") AttachmentType attachmentType);

    @Modifying
    @Query("UPDATE Attachment a SET a.isDeleted = true WHERE a.attachmentId = :attachmentId")
    void softDeleteById(@Param("attachmentId") Long attachmentId);

}
