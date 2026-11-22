package com.iitsm.repository;

import com.iitsm.entity.Assignment;
import org.springframework.data.jpa.repository.*;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
  java.util.List<Assignment> findByIncidentIdOrderByCreatedAtAsc(Long id);
}
