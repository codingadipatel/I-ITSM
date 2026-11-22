package com.iitsm.repository;

import com.iitsm.entity.Incident;
import org.springframework.data.jpa.repository.*;

public interface IncidentRepository
    extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
  java.util.List<Incident> findByCreatedAtAfter(java.time.Instant date);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select i from Incident i where i.id=:id")
  java.util.Optional<Incident> findLockedById(
      @org.springframework.data.repository.query.Param("id") Long id);

  long countByTechnicianIdAndCategoryIdAndStatusIn(
      Long technicianId, Long categoryId, java.util.Collection<com.iitsm.model.Status> statuses);

  long countByTechnicianIdAndStatusNotIn(
      Long id, java.util.Collection<com.iitsm.model.Status> statuses);
}
