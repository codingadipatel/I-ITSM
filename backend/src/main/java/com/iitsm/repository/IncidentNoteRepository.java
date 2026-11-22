package com.iitsm.repository;

import com.iitsm.entity.IncidentNote;
import org.springframework.data.jpa.repository.*;

public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {
  java.util.List<IncidentNote> findByIncidentIdOrderByCreatedAtAsc(Long id);
}
