package com.iitsm.strategy;

import com.iitsm.entity.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class SkillBasedAssignmentStrategy implements TechnicianAssignmentStrategy {
  public String key() {
    return "SKILL";
  }

  public Optional<Recommendation> recommend(Incident i, List<Candidate> candidates) {
    return candidates.stream()
        .filter(Candidate::eligible)
        .map(
            c -> {
              Set<String> skills = new HashSet<>();
              c.technician().getSkills().forEach(s -> skills.add(s.getName().toLowerCase()));
              int matches =
                  (skills.contains(i.getCategory().getRequiredSkill().toLowerCase()) ? 1 : 0)
                      + (skills.contains(i.getService().getRequiredSkill().toLowerCase()) ? 1 : 0);
              double score =
                  matches * 100
                      + c.technician().getExperienceYears() * 2
                      + Math.min(c.resolvedInCategory(), 10) * 3
                      - c.workload() * 5;
              return new Recommendation(
                  c.technician(),
                  matches
                      + " / 2 skill matches, "
                      + c.technician().getExperienceYears()
                      + " years experience, "
                      + c.resolvedInCategory()
                      + " resolved in category, "
                      + c.workload()
                      + " / "
                      + c.technician().getMaxWorkload()
                      + " active incidents; score "
                      + score,
                  score);
            })
        .max(
            Comparator.comparingDouble(Recommendation::score)
                .thenComparing(r -> r.technician().getEmail()));
  }
}
