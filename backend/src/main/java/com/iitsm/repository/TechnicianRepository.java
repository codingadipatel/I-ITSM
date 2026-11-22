package com.iitsm.repository;

import com.iitsm.entity.Technician;
import org.springframework.data.jpa.repository.*;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Technician t where t.id=:id")
  java.util.Optional<Technician> findLockedById(
      @org.springframework.data.repository.query.Param("id") Long id);
}
