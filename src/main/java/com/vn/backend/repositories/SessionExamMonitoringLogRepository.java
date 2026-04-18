package com.vn.backend.repositories;

import com.vn.backend.entities.SessionExamMonitoringLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionExamMonitoringLogRepository extends
    JpaRepository<SessionExamMonitoringLog, Long> {

  @Query("select l from SessionExamMonitoringLog l where l.sessionExam.sessionExamId = :id")
  Optional<SessionExamMonitoringLog> findBySessionExamId(@Param("id") Long id);
}
