package com.iitsm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter
@NoArgsConstructor
public class SystemSettings {
  @Id private Long id = 1L;
  private String assignmentStrategy = "SKILL";
  private String priorityStrategy = "WEIGHTED";
  private double p1Threshold = 24;
  private double p2Threshold = 12;
  private double p3Threshold = 5;
  private boolean emailEnabled = true;

  public void configure(
      String assignment, String priority, double p1, double p2, double p3, boolean email) {
    if (!java.util.Set.of("SKILL", "LOAD").contains(assignment)
        || !java.util.Set.of("WEIGHTED", "RULE").contains(priority)
        || !(p1 > p2 && p2 > p3 && p3 > 0))
      throw new IllegalArgumentException(
          "Invalid strategy or thresholds: P1 > P2 > P3 > 0 required");
    assignmentStrategy = assignment;
    priorityStrategy = priority;
    p1Threshold = p1;
    p2Threshold = p2;
    p3Threshold = p3;
    emailEnabled = email;
  }
}
