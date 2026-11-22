package com.iitsm.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.iitsm.entity.*;
import com.iitsm.strategy.TechnicianAssignmentStrategy.*;
import java.util.*;
import org.junit.jupiter.api.*;

class AssignmentStrategyTest {
  Incident incident() {
    return new Incident(
        new Employee("A", "a@b.test", "hash"),
        "Network failure",
        "The office network is offline",
        new ServiceItem("Network", 3, "Network"),
        new IncidentCategory("Connectivity", "Network"),
        3,
        3,
        100);
  }

  Technician technician(String email, boolean available, Set<Skill> skills) {
    var t = new Technician(email, email, "hash");
    t.configure(available, 5, 3, skills);
    return t;
  }

  @Test
  void lowestEligibleLoad() {
    var a = technician("a@b", true, Set.of());
    var b = technician("b@b", true, Set.of());
    assertSame(
        b,
        new LoadBasedAssignmentStrategy()
            .recommend(incident(), List.of(new Candidate(a, 4), new Candidate(b, 1)))
            .orElseThrow()
            .technician());
  }

  @Test
  void unavailableAndAtCapacityExcluded() {
    var t = technician("a@b", false, Set.of());
    var full = technician("b@b", true, Set.of());
    assertTrue(
        new LoadBasedAssignmentStrategy()
            .recommend(incident(), List.of(new Candidate(t, 0), new Candidate(full, 5)))
            .isEmpty());
  }

  @Test
  void matchingExpertiseOutweighsSmallLoadDifference() {
    var expert = technician("a@b", true, Set.of(new Skill("Network")));
    var other = technician("b@b", true, Set.of(new Skill("Hardware")));
    var r =
        new SkillBasedAssignmentStrategy()
            .recommend(incident(), List.of(new Candidate(expert, 4), new Candidate(other, 0)))
            .orElseThrow();
    assertSame(expert, r.technician());
    assertTrue(r.reason().contains("2 / 2 skill matches"));
  }

  @Test
  void inactiveTechnicianExcluded() {
    var t = technician("a@b", true, Set.of());
    t.update("A", false);
    assertFalse(new Candidate(t, 0).eligible());
  }
}
