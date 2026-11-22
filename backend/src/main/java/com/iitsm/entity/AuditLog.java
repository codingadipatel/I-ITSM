package com.iitsm.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne private Incident incident;
  @ManyToOne private User actor;
  private String action;
  private String previousState;
  private String newState;

  @Column(columnDefinition = "text")
  private String description;

  private Instant createdAt = Instant.now();

  public AuditLog(
      Incident i, User a, String action, String before, String after, String description) {
    incident = i;
    actor = a;
    this.action = action;
    previousState = before;
    newState = after;
    this.description = description;
  }
}
