package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "incident_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne private Incident incident;
  @ManyToOne private Technician technician;
  @ManyToOne private User assignedBy;
  private String reason;
  private Instant createdAt = Instant.now();

  public Assignment(Incident i, Technician t, User u, String r) {
    incident = i;
    technician = t;
    assignedBy = u;
    reason = r;
  }
}
