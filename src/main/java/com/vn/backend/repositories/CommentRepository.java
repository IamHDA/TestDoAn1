package com.vn.backend.repositories;

import com.vn.backend.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find comments by announcement ID (not deleted)
    @Query("SELECT c FROM Comment c WHERE c.announcementId = :announcementId AND c.isDeleted = false")
    Page<Comment> findByAnnouncementIdAndNotDeleted(@Param("announcementId") Long announcementId, Pageable pageable);


    // Find comment by ID (not deleted)
    @Query("SELECT c FROM Comment c WHERE c.commentId = :commentId AND c.isDeleted = false")
    Optional<Comment> findByCommentIdAndNotDeleted(@Param("commentId") Long commentId);

    // Soft delete comment
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.commentId = :commentId")
    void softDeleteById(@Param("commentId") Long commentId);
}
