package com.iitsm.strategy;

import com.iitsm.entity.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class LoadBasedAssignmentStrategy implements TechnicianAssignmentStrategy {
  public String key() {
    return "LOAD";
  }

  public Optional<Recommendation> recommend(Incident incident, List<Candidate> candidates) {
    return candidates.stream()
        .filter(Candidate::eligible)
        .min(
            Comparator.comparingLong(Candidate::workload)
                .thenComparing(c -> c.technician().getEmail()))
        .map(
            c ->
                new Recommendation(
                    c.technician(),
                    "Lowest active workload: "
                        + c.workload()
                        + " / "
                        + c.technician().getMaxWorkload(),
                    -c.workload()));
  }
}
