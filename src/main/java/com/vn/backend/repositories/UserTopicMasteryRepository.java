package com.vn.backend.repositories;

import com.vn.backend.entities.UserTopicMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTopicMasteryRepository extends JpaRepository<UserTopicMastery, Long> {

    Optional<UserTopicMastery> findByUserIdAndTopicIdAndIsDeletedFalse(Long userId, Long topicId);

    List<UserTopicMastery> findByUserIdAndIsDeletedFalse(Long userId);

    @Query("SELECT utm FROM UserTopicMastery utm WHERE utm.userId = :userId AND utm.topicId IN :topicIds AND utm.isDeleted = false")
    List<UserTopicMastery> findByUserIdAndTopicIdIn(@Param("userId") Long userId, @Param("topicIds") List<Long> topicIds);
}

