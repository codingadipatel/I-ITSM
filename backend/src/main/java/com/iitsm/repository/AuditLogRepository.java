package com.iitsm.repository;

import com.iitsm.entity.AuditLog;
import org.springframework.data.jpa.repository.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  java.util.List<AuditLog> findByIncidentIdOrderByCreatedAtAsc(Long id);
}
