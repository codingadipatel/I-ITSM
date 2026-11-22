package com.iitsm.repository;

import com.iitsm.entity.DuplicateSuggestion;
import org.springframework.data.jpa.repository.*;

public interface DuplicateSuggestionRepository extends JpaRepository<DuplicateSuggestion, Long> {
  java.util.List<DuplicateSuggestion> findByIncidentIdOrRelatedId(Long incidentId, Long relatedId);
}
