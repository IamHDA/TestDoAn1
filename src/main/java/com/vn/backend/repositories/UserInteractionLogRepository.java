package com.vn.backend.repositories;

import com.vn.backend.entities.UserInteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInteractionLogRepository extends JpaRepository<UserInteractionLog, Long> {

    List<UserInteractionLog> findByUserIdAndIsDeletedFalseOrderByInteractionTimestampDesc(Long userId);

    List<UserInteractionLog> findByUserIdAndQuestionIdAndIsDeletedFalseOrderByInteractionTimestampDesc(Long userId, Long questionId);
}

