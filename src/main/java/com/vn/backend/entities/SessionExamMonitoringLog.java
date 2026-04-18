package com.vn.backend.entities;

import com.vn.backend.dto.response.sessionexam.SessionExamMonitoringResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "session_exam_monitoring_logs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionExamMonitoringLog extends BaseEntity {

  @Id
  @Column(name = "session_exam_id")
  private Long sessionExamId;

  @Column(name = "monitoring_data", columnDefinition = "JSON")
  @JdbcTypeCode(SqlTypes.JSON)
  private SessionExamMonitoringResponse monitoringData;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_exam_id", insertable = false, updatable = false)
  private SessionExam sessionExam;
}