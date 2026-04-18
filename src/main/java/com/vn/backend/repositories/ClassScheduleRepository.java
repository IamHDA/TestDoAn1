package com.vn.backend.repositories;

import com.vn.backend.entities.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Integer> {
}
