package com.iitsm.strategy;

import com.iitsm.entity.*;
import java.util.*;

public interface TechnicianAssignmentStrategy {
  String key();

  Optional<Recommendation> recommend(Incident incident, List<Candidate> candidates);

  record Candidate(Technician technician, long workload, int resolvedInCategory) {
    public Candidate(Technician technician, long workload) {
      this(technician, workload, 0);
    }

    public boolean eligible() {
      return technician.isActive()
          && technician.isAvailable()
          && workload < technician.getMaxWorkload();
    }
  }

  record Recommendation(Technician technician, String reason, double score) {}
}
