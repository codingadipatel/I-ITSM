package com.iitsm.repository;

import com.iitsm.entity.Feedback;
import org.springframework.data.jpa.repository.*;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
  java.util.Optional<Feedback> findByIncidentId(Long id);
}
