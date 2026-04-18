package com.vn.backend.repositories;

import com.vn.backend.entities.ClassroomSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassroomSettingRepository extends JpaRepository<ClassroomSetting, Long> {
  Optional<ClassroomSetting> findByClassroomId(Long classroomId);

  @Query("""
        select cs
        from ClassroomSetting cs
        where cs.classroomId = :classroomId
        and cs.classroom.teacherId = :teacherId
      """)
  Optional<ClassroomSetting> findByClassroomIdAndTeacherId(Long classroomId, Long teacherId);


  boolean existsByClassroomIdAndAllowStudentPostFalse(Long classroomId);
}
