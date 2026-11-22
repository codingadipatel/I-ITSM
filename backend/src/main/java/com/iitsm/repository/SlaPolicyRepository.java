package com.iitsm.repository;

import com.iitsm.entity.SlaPolicy;
import org.springframework.data.jpa.repository.*;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
  java.util.Optional<SlaPolicy> findByPriority(com.iitsm.model.Priority priority);
}
